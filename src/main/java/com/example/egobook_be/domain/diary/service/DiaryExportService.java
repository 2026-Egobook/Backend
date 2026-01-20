package com.example.egobook_be.domain.diary.service;

import com.example.egobook_be.domain.diary.entity.Diary;
import com.example.egobook_be.domain.diary.enums.DiaryType;
import com.example.egobook_be.domain.diary.exception.DiaryErrorCode;
import com.example.egobook_be.domain.user.entity.User;
import com.example.egobook_be.global.exception.CustomException;
import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.AreaBreak;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.AreaBreakType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DiaryExportService {

    /** PDF 파일 생성 */
    public byte[] generatePdf(List<Diary> diaries, User user) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf, PageSize.A4);

            try {
                // ClassPathResource를 사용하여 JAR 배포 환경에서도 파일을 읽을 수 있게 함
                ClassPathResource fontResource = new ClassPathResource("fonts/NanumGothic.ttf");

                // 파일을 바이트 배열로 읽어서 폰트 생성 (가장 안전한 방법)
                byte[] fontBytes = fontResource.getInputStream().readAllBytes();

                // IDENTITY_H: 수평 쓰기 한글 인코딩
                // FORCE_EMBEDDED: 폰트를 PDF 안에 포함시킴 (깨짐 방지)
                PdfFont font = PdfFontFactory.createFont(
                        fontBytes,
                        PdfEncodings.IDENTITY_H,
                        PdfFontFactory.EmbeddingStrategy.FORCE_EMBEDDED
                );
                document.setFont(font);

            } catch (Exception e) {
                log.error("폰트 로드 실패. 기본 폰트로 진행합니다 (한글 깨짐 가능성 있음).", e);
            }

            // 날짜별로 그룹화
            Map<LocalDate, List<Diary>> diariesByDate = diaries.stream()
                    .collect(Collectors.groupingBy(
                            diary -> diary.getWrittenAt().toLocalDate(),
                            Collectors.toList()
                    ));

            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy. MM. dd");
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

            boolean isFirstPage = true;

            // 날짜별로 페이지 생성
            for (Map.Entry<LocalDate, List<Diary>> entry : diariesByDate.entrySet()) {
                LocalDate date = entry.getKey();
                List<Diary> dayDiaries = entry.getValue();

                // 첫 페이지가 아니면 페이지 구분
                if (!isFirstPage) {
                    document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));
                }
                isFirstPage = false;

                // 날짜 헤더
                Paragraph dateHeader = new Paragraph(date.format(dateFormatter) + "일기")
                        .setFontSize(20)
                        .setBold()
                        .setMarginBottom(30);
                document.add(dateHeader);

                // 해당 날짜 일기
                dayDiaries.stream()
                        .sorted((d1, d2) -> d2.getWrittenAt().compareTo(d1.getWrittenAt()))
                        .forEach(diary -> {
                            // 일기 저장 시간, 일기 타입
                            Text timeText = new Text(diary.getWrittenAt().format(timeFormatter))
                                    .setBold();

                            Text typeText = new Text(" (" + diary.getType().stream()
                                    .map(this::getDiaryTypeText)
                                    .collect(Collectors.joining(", ")) + ")");

                            Paragraph para = new Paragraph()
                                    .add(timeText)
                                    .add(typeText)
                                    .setFontSize(14)
                                    .setMarginTop(15)
                                    .setMarginBottom(10);

                            document.add(para);

                            // 감정 단계 이모티콘
                            if (diary.getEmotionLevel() != null) {
                                try {
                                    Image emotionImage = getEmotionImage(diary.getEmotionLevel());
                                    if (emotionImage != null) {
                                        emotionImage.setMarginBottom(10);
                                        emotionImage.setWidth(30);
                                        emotionImage.setHeight(30);
                                        document.add(emotionImage);
                                    }
                                } catch (Exception e) {
                                    Paragraph emotionPara = new Paragraph("감정 단계: " + diary.getEmotionLevel())
                                            .setFontSize(10)
                                            .setMarginBottom(8);
                                    document.add(emotionPara);
                                }
                            }

                            // 일기 내용
                            Paragraph contentPara = new Paragraph(diary.getContent())
                                    .setFontSize(11)
                                    .setMarginBottom(20)
                                    .setPaddingLeft(15)
                                    .setBorderLeft(new SolidBorder(ColorConstants.LIGHT_GRAY, 2));
                            document.add(contentPara);
                        });
            }

            document.close();
            return baos.toByteArray();

        } catch (Exception e) {
            throw new CustomException(DiaryErrorCode.DIARY_EXPORT_FAILED);
        }
    }

    /** TEXT 파일 생성 */
    public byte[] generateText(List<Diary> diaries, User user) {
        StringBuilder sb = new StringBuilder();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy. MM. dd");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

        // 날짜에 따라 나누기
        Map<LocalDate, List<Diary>> diariesByDate = diaries.stream()
                .collect(Collectors.groupingBy(
                        diary -> diary.getWrittenAt().toLocalDate(),
                        Collectors.toList()
                ));

        diariesByDate.forEach((date, dayDiaries) -> {
            sb.append(date.format(dateFormatter)).append(" 일기\n");
            sb.append("==========================================\n\n");

            // 해당 날짜 일기
            dayDiaries.stream()
                    .sorted((d1, d2) -> d2.getWrittenAt().compareTo(d1.getWrittenAt()))
                    .forEach(diary -> {
                        sb.append(diary.getWrittenAt().format(timeFormatter)).append(" ");

                        // 일기 타입
                        String types = diary.getType().stream()
                                .map(this::getDiaryTypeText)
                                .collect(Collectors.joining(", "));
                        sb.append("(").append(types).append(")\n");

                        // 감정 레벨
                        if (diary.getEmotionLevel() != null) {
                            sb.append("기분: ").append("★".repeat(diary.getEmotionLevel())).append("\n");
                        }
                        sb.append(diary.getContent());
                        sb.append("\n\n");
                    });

        });

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    /** emotionLevel에 맞는 이미지 반환 */
    private Image getEmotionImage(Integer emotionLevel) {
        try {
            String imagePath = "images/emotions/emotion_" + emotionLevel + ".png";
            ClassPathResource resource = new ClassPathResource(imagePath);

            if (resource.exists()) {
                // [수정] InputStream을 try (...) 안에 넣어서 자동으로 close 되도록 함
                try (java.io.InputStream inputStream = resource.getInputStream()) {
                    byte[] imageBytes = inputStream.readAllBytes();
                    return new Image(ImageDataFactory.create(imageBytes));
                }
            }
            return null;
        } catch (Exception e) {
            // 이미지가 없거나 읽을 수 없으면 무시하고 텍스트로 대체됨
            return null;
        }
    }

    /** DiaryType을 텍스트로 변환 */
    private String getDiaryTypeText(DiaryType type) {
        return switch (type) {
            case EMOTION -> "감정";
            case CONCERN -> "고민";
            case PRAISE -> "칭찬";
            case GRATITUDE -> "감사";
        };
    }
}