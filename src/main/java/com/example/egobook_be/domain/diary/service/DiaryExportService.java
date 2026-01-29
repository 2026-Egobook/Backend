package com.example.egobook_be.domain.diary.service;

import com.example.egobook_be.domain.diary.entity.Diary;
import com.example.egobook_be.domain.diary.enums.DiaryType;
import com.example.egobook_be.domain.diary.exception.DiaryErrorCode;
import com.example.egobook_be.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DiaryExportService {

    private static final float MARGIN = 50;
    private static final float FONT_SIZE_TITLE = 20;
    private static final float FONT_SIZE_TIME = 14;
    private static final float FONT_SIZE_TYPE = 11;
    private static final float FONT_SIZE_CONTENT = 11;
    private static final float LINE_HEIGHT = 1.5f;
    private static final byte[] FONT;
    private static final Map<Integer, byte[]> EMOTION_IMG = new HashMap<>();

    static {
        // 폰트 데이터 캐싱
        try {
            ClassPathResource fontResource = new ClassPathResource("fonts/NanumGothic.ttf");

            if (fontResource.exists()) {
                try (InputStream fontStream = fontResource.getInputStream()) {
                    FONT = fontStream.readAllBytes();
                }
            } else {
                FONT = null;
            }
        } catch (Exception e) {
            throw new ExceptionInInitializerError("폰트 로딩 실패: " + e.getMessage());
        }

        // 이모티콘 캐싱
        for (int level = 1; level <= 5; level++) {
            try {
                String imagePath = "images/emotions/emotion_" + level + ".png";
                ClassPathResource resource = new ClassPathResource(imagePath);

                if (resource.exists()) {
                    try (InputStream imageStream = resource.getInputStream()) {
                        EMOTION_IMG.put(level, imageStream.readAllBytes());
                    }
                }
            } catch (Exception e) {
                log.warn("이모티콘 이미지 로딩 실패: emotion_{}.png", level);
            }
        }
    }

    /** PDF 파일 생성 */
    public byte[] generatePdf(List<Diary> diaries) {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            // 한글 폰트
            PDFont font;
            if (FONT != null) {
                font = PDType0Font.load(document, new ByteArrayInputStream(FONT));
            } else {
                font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            }

            // 날짜 그룹화
            Map<LocalDate, List<Diary>> diariesByDate = diaries.stream()
                    .sorted(Comparator.comparing(Diary::getDate))
                    .collect(Collectors.groupingBy(
                            Diary::getDate,
                            LinkedHashMap::new,
                            Collectors.toList()
                    ));

            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy. MM. dd");
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

            PDPage currentPage = null;
            PDPageContentStream contentStream = null;
            float yPosition = 0;

            // 날짜별 처리
            for (Map.Entry<LocalDate, List<Diary>> entry : diariesByDate.entrySet()) {
                LocalDate date = entry.getKey();
                List<Diary> dayDiaries = entry.getValue();

                // 새 페이지 시작
                if (contentStream != null) {
                    contentStream.close();
                }
                currentPage = new PDPage(PDRectangle.A4);
                document.addPage(currentPage);
                contentStream = new PDPageContentStream(document, currentPage);
                yPosition = currentPage.getMediaBox().getHeight() - MARGIN - 10;

                // 날짜
                contentStream.beginText();
                contentStream.setFont(font, FONT_SIZE_TITLE);
                contentStream.newLineAtOffset(MARGIN, yPosition);
                contentStream.showText(date.format(dateFormatter) + "  일기");
                contentStream.endText();
                yPosition -= FONT_SIZE_TITLE * LINE_HEIGHT + 20;


                for (Diary diary : dayDiaries) {
                    // 시간, 일기 타입
                    String types = diary.getType().stream()
                            .map(this::getDiaryTypeText)
                            .collect(Collectors.joining(", "));
                    String timeAndType = diary.getWrittenAt().format(timeFormatter) + " (" + types + ")";

                    // 페이지 넘어가는지 확인
                    if (yPosition < MARGIN + 100) {
                        contentStream.close();
                        currentPage = new PDPage(PDRectangle.A4);
                        document.addPage(currentPage);
                        contentStream = new PDPageContentStream(document, currentPage);
                        yPosition = currentPage.getMediaBox().getHeight() - MARGIN -10;
                    }

                    contentStream.beginText();
                    contentStream.setFont(font, FONT_SIZE_TIME);
                    contentStream.newLineAtOffset(MARGIN, yPosition);
                    contentStream.showText(timeAndType);
                    contentStream.endText();
                    yPosition -= FONT_SIZE_TIME * LINE_HEIGHT - 5;

                    // 감정 이모티콘
                    if (diary.getEmotionLevel() != null) {
                        try {
                            PDImageXObject image = loadEmotionImage(document, diary.getEmotionLevel());
                            if (image != null) {
                                contentStream.drawImage(image, MARGIN, yPosition - 25, 25, 25);
                                yPosition -= 30;
                            } else {
                                contentStream.beginText();
                                contentStream.setFont(font, FONT_SIZE_TYPE);
                                contentStream.newLineAtOffset(MARGIN, yPosition);
                                contentStream.showText("기분: " + diary.getEmotionLevel());
                                contentStream.endText();
                                yPosition -= FONT_SIZE_TYPE * LINE_HEIGHT + 5;
                            }
                        } catch (Exception e) {
                            contentStream.beginText();
                            contentStream.setFont(font, FONT_SIZE_TYPE);
                            contentStream.newLineAtOffset(MARGIN, yPosition);
                            contentStream.showText("기분: " + diary.getEmotionLevel());
                            contentStream.endText();
                            yPosition -= FONT_SIZE_TYPE * LINE_HEIGHT + 5;
                        }
                    }

                    // 일기 내용
                    yPosition -= 10;
                    List<String> lines = wrapText(diary.getContent(), font,
                            currentPage.getMediaBox().getWidth() - MARGIN * 2 - 20);

                    for (String line : lines) {
                        // 페이지 넘어가는지 확인
                        if (yPosition < MARGIN + 20) {
                            contentStream.close();
                            currentPage = new PDPage(PDRectangle.A4);
                            document.addPage(currentPage);
                            contentStream = new PDPageContentStream(document, currentPage);
                            yPosition = currentPage.getMediaBox().getHeight() - MARGIN -10;
                        }

                        contentStream.beginText();
                        contentStream.setFont(font, FONT_SIZE_CONTENT);
                        contentStream.newLineAtOffset(MARGIN, yPosition);
                        contentStream.showText(line);
                        contentStream.endText();
                        yPosition -= FONT_SIZE_CONTENT * LINE_HEIGHT;
                    }

                    yPosition -= 20; // 일기 간격
                }
            }

            if (contentStream != null) {
                contentStream.close();
            }

            document.save(baos);
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("PDF 생성 실패: {}", e.getMessage(), e);
            throw new CustomException(DiaryErrorCode.DIARY_EXPORT_FAILED);
        }
    }

    /** TEXT 파일 생성 */
    public byte[] generateText(List<Diary> diaries) {
        StringBuilder sb = new StringBuilder();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy. MM. dd");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

        Map<LocalDate, List<Diary>> diariesByDate = diaries.stream()
                .collect(Collectors.groupingBy(
                        Diary::getDate,
                        TreeMap::new,
                        Collectors.toList()
                ));

        diariesByDate.forEach((date, dayDiaries) -> {
            sb.append(date.format(dateFormatter)).append(" 일기\n");
            sb.append("==========================================\n\n");

            dayDiaries.stream()
                    .sorted((d1, d2) -> d2.getWrittenAt().compareTo(d1.getWrittenAt()))
                    .forEach(diary -> {
                        String types = diary.getType().stream()
                                .map(this::getDiaryTypeText)
                                .collect(Collectors.joining(", "));
                        sb.append(diary.getWrittenAt().format(timeFormatter))
                                .append(" (").append(types).append(")\n");

                        if (diary.getEmotionLevel() != null) {
                            sb.append("기분: ").append("★".repeat(diary.getEmotionLevel())).append("\n");
                        }
                        sb.append(diary.getContent());
                        sb.append("\n\n");
                    });
        });

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private PDImageXObject loadEmotionImage(PDDocument document, Integer emotionLevel) {
        try {
            byte[] imageData = EMOTION_IMG.get(emotionLevel);
            if (imageData != null) {
                return PDImageXObject.createFromByteArray(document, imageData, "emotion");
            }
            return null;
        } catch (Exception e) {
            log.error("이미지 변환 실패: emotion_{}", emotionLevel, e);
            return null;
        }
    }

    private List<String> wrapText(String text, PDFont font, float maxWidth) throws Exception {
        List<String> lines = new ArrayList<>();
        String[] paragraphs = text.split("\n");

        for (String paragraph : paragraphs) {
            if (paragraph.trim().isEmpty()) {
                lines.add("");
                continue;
            }

            String[] words = paragraph.split(" ");
            StringBuilder currentLine = new StringBuilder();

            for (String word : words) {
                String testLine = currentLine.length() == 0 ? word : currentLine + " " + word;
                float width = font.getStringWidth(testLine) / 1000 * FONT_SIZE_CONTENT;

                if (width > maxWidth && currentLine.length() > 0) {
                    lines.add(currentLine.toString());
                    currentLine = new StringBuilder(word);
                } else {
                    currentLine = new StringBuilder(testLine);
                }
            }

            if (currentLine.length() > 0) {
                lines.add(currentLine.toString());
            }
        }

        return lines;
    }

    private String getDiaryTypeText(DiaryType type) {
        return switch (type) {
            case EMOTION -> "감정";
            case CONCERN -> "고민";
            case PRAISE -> "칭찬";
            case GRATITUDE -> "감사";
        };
    }
}