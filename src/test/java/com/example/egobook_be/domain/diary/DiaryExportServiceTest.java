package com.example.egobook_be.domain.diary;

import com.example.egobook_be.domain.diary.entity.Diary;
import com.example.egobook_be.domain.diary.enums.DiaryType;
import com.example.egobook_be.domain.diary.service.DiaryExportService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class DiaryExportServiceTest {

    @InjectMocks
    private DiaryExportService diaryExportService;

    @Nested
    @DisplayName("TEXT 내보내기 : generateText()")
    class GenerateTextTest {

        @Test
        @DisplayName("일기 리스트 정상 PDF 파일 생성")
        void generateText_Success() {
            // Given
            Diary diary1 = Diary.builder()
                    .date(LocalDate.of(2025, 5, 5))
                    .writtenAt(LocalDateTime.of(2025, 5, 5, 12, 0))
                    .type(Set.of(DiaryType.EMOTION))
                    .emotionLevel(5)
                    .content("아 배고파 아 힘들어")
                    .build();

            Diary diary2 = Diary.builder()
                    .date(LocalDate.of(2025, 5, 6))
                    .writtenAt(LocalDateTime.of(2025, 5, 6, 20, 30))
                    .type(Set.of(DiaryType.CONCERN))
                    .content("엄마빠 보고싶다")
                    .build();

            // When
            String content = toText(diaryExportService.generateText(List.of(diary1, diary2)));

            // Then
            assertThat(content).contains("2025. 05. 05");
            assertThat(content).contains("2025. 05. 06");
            assertThat(content).contains("아 배고파 아 힘들어");
            assertThat(content).contains("엄마빠 보고싶다");
            assertThat(content).contains("★★★★★");
            assertThat(content).contains("(감정)");
            assertThat(content).contains("(고민)");
        }

        @Test
        @DisplayName("감정 단계 없는 일기 TEXT 변환 시 별점 미포함")
        void generateText_NoEmotionLevel_Success() {
            // Given
            Diary diary = Diary.builder()
                    .date(LocalDate.now())
                    .writtenAt(LocalDateTime.now())
                    .type(Set.of(DiaryType.GRATITUDE))
                    .emotionLevel(null)
                    .content("감정 점수 없는 일기")
                    .build();

            // When
            String content = toText(diaryExportService.generateText(List.of(diary)));

            // Then
            assertThat(content).contains("감정 점수 없는 일기");
            assertThat(content).doesNotContain("기분:");
            assertThat(content).doesNotContain("★");
        }

        @Test
        @DisplayName("일기 타입이 여러 개일 경우 TEXT에 모두 포함")
        void generateText_MultipleTypes_Success() {
            // Given
            Diary diary = Diary.builder()
                    .date(LocalDate.now())
                    .writtenAt(LocalDateTime.now())
                    .type(Set.of(DiaryType.EMOTION, DiaryType.CONCERN))
                    .emotionLevel(3)
                    .content("여러 감정 일기")
                    .build();

            // When
            String content = toText(diaryExportService.generateText(List.of(diary)));

            // Then
            assertThat(content).contains("감정");
            assertThat(content).contains("고민");
        }

        @Test
        @DisplayName("내용이 빈 문자열인 일기 TEXT 변환 시 날짜, 타입 정상 포함")
        void generateText_EmptyContent_Success() {
            // Given
            Diary diary = Diary.builder()
                    .date(LocalDate.of(2025, 1, 1))
                    .writtenAt(LocalDateTime.of(2025, 1, 1, 12, 0))
                    .type(Set.of(DiaryType.PRAISE))
                    .emotionLevel(1)
                    .content("")
                    .build();

            // When
            String content = toText(diaryExportService.generateText(List.of(diary)));

            // Then
            assertThat(content).contains("2025");
            assertThat(content).contains("(칭찬)");
        }
    }

    @Nested
    @DisplayName("PDF 내보내기 : generatePdf()")
    class GeneratePdfTest {

        @Test
        @DisplayName("일기 리스트 정상 PDF 파일 생성")
        void generatePdf_Success() {
            // Given
            Diary diary = Diary.builder()
                    .date(LocalDate.now())
                    .writtenAt(LocalDateTime.now())
                    .type(Set.of(DiaryType.EMOTION))
                    .emotionLevel(3)
                    .content("아 졸려 아 집가고싶어")
                    .build();

            // When
            byte[] result = diaryExportService.generatePdf(List.of(diary));

            // Then
            assertThat(result).isNotEmpty();
            assertThat(toPdfHeader(result)).isEqualTo("%PDF");
        }

        @Test
        @DisplayName("감정 단계 없는 일기 정상 PDF 파일 생성")
        void generatePdf_NoEmotionLevel_Success() {
            // Given
            Diary diary = Diary.builder()
                    .date(LocalDate.now())
                    .writtenAt(LocalDateTime.now())
                    .type(Set.of(DiaryType.GRATITUDE))
                    .emotionLevel(null)
                    .content("이미지 없는 일기")
                    .build();

            // When
            byte[] result = diaryExportService.generatePdf(List.of(diary));

            // Then
            assertThat(result).isNotEmpty();
            assertThat(toPdfHeader(result)).isEqualTo("%PDF");
        }

        @Test
        @DisplayName("긴 일기 정상 PDF 파일 생성")
        void generatePdf_LongContent_Success() {
            // Given
            Diary diary = Diary.builder()
                    .date(LocalDate.now())
                    .writtenAt(LocalDateTime.now())
                    .type(Set.of(DiaryType.GRATITUDE))
                    .content("아주 긴 일기".repeat(200))
                    .build();

            // When
            byte[] result = diaryExportService.generatePdf(List.of(diary));

            // Then
            assertThat(result).isNotEmpty();
            assertThat(toPdfHeader(result)).isEqualTo("%PDF");
        }
    }

    private String toText(byte[] bytes) {
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private String toPdfHeader(byte[] bytes) {
        return new String(bytes, 0, 4, StandardCharsets.UTF_8);
    }
}
