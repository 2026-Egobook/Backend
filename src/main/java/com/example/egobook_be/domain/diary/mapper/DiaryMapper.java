package com.example.egobook_be.domain.diary.mapper;

import com.example.egobook_be.domain.diary.dto.*;
import com.example.egobook_be.domain.diary.entity.Diary;
import com.example.egobook_be.domain.diary.enums.ExportFormat;
import com.example.egobook_be.domain.diary.repository.DiaryRepository;
import com.example.egobook_be.global.response.SliceResponse;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DiaryMapper {
    public static DiaryResDto toDiaryDto(Diary diary) {
        return DiaryResDto.builder()
                .diaryId(diary.getId())
                .date(diary.getDate())
                .writtenAt(diary.getWrittenAt())
                .type(diary.getType())
                .emotionLevel(diary.getEmotionLevel())
                .content(diary.getContent())
                .createdAt(diary.getCreatedAt())
                .updatedAt(diary.getUpdatedAt())
                .build();
    }

    public static DiaryCreateResDto toDiaryCreateDto(Diary diary, List<DiaryCreateResDto.RewardResDto> rewards) {
        return DiaryCreateResDto.builder()
                .entry(DiaryCreateResDto.DiaryEntryResDto.builder()
                        .diaryId(diary.getId())
                        .date(diary.getWrittenAt().toLocalDate())
                        .writtenAt(diary.getWrittenAt())
                        .type(diary.getType())
                        .emotionLevel(diary.getEmotionLevel())
                        .content(diary.getContent())
                        .createdAt(diary.getCreatedAt())
                        .updatedAt(diary.getUpdatedAt())
                        .build())
                .rewards(rewards)
                .build();
    }

    public static DiaryListResDto toDiaryListDto(SliceResponse<DiaryResDto> diaries, int dailyCount) {
        return DiaryListResDto.builder()
                .diaries(diaries)
                .dailyCount(dailyCount)
                .build();
    }

    public static DiaryCalendarResDto toDiaryCalendarDto(YearMonth month, List<DiaryRepository.DailyEmotionCount> dailyEmotions) {

        Map<LocalDate, DiaryRepository.DailyEmotionCount> topEmotions =
                dailyEmotions.stream()
                        .collect(Collectors.toMap(
                                DiaryRepository.DailyEmotionCount::getDate,
                                e -> e,
                                (exist, replace) -> exist
                        ));

        List<DiaryCalendarResDto.DailyTopEmotionResDto> days =
                topEmotions.values().stream()
                        .map(e -> DiaryCalendarResDto.DailyTopEmotionResDto.builder()
                                .date(e.getDate())
                                .emotionLevel(e.getEmotionLevel())
                                .build()
                        )
                        .sorted(Comparator.comparing(DiaryCalendarResDto.DailyTopEmotionResDto::date))
                        .toList();

        return DiaryCalendarResDto.builder()
                .month(month)
                .days(days)
                .build();
    }

    public static DiaryExportResDto toDiaryExportDto(
            String fileUrl, LocalDateTime expiresAt, ExportFormat format, LocalDate startDate, LocalDate endDate
    ) {
        return DiaryExportResDto.builder()
                .fileUrl(fileUrl)
                .expiresAt(expiresAt)
                .format(format)
                .range(new DiaryExportResDto.DateRange(startDate, endDate))
                .build();
    }

    public static DiaryDeleteResDto toDiaryDeleteDto(boolean deleted) {
        return DiaryDeleteResDto.builder()
                .deleted(deleted)
                .build();
    }
}
