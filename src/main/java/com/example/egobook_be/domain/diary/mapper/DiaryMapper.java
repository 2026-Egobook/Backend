package com.example.egobook_be.domain.diary.mapper;

import com.example.egobook_be.domain.diary.dto.DiaryCreateResDto;
import com.example.egobook_be.domain.diary.dto.DiaryListResDto;
import com.example.egobook_be.domain.diary.dto.DiaryResDto;
import com.example.egobook_be.domain.diary.entity.Diary;
import com.example.egobook_be.global.response.SliceResponse;

import java.util.List;

public class DiaryMapper {
    public static DiaryResDto toDiaryResDto(Diary diary) {
        return DiaryResDto.builder()
                .diaryId(diary.getId())
                .date(diary.getWrittenAt().toLocalDate())
                .writtenAt(diary.getWrittenAt())
                .type(diary.getType())
                .emotionLevel(diary.getEmotionLevel())
                .content(diary.getContent())
                .createdAt(diary.getCreatedAt())
                .updatedAt(diary.getUpdatedAt())
                .build();
    }

    public static DiaryCreateResDto toDiaryCreateResDto(Diary diary, List<DiaryCreateResDto.RewardResDto> rewards) {
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

    public static DiaryListResDto toDiaryListResDto(SliceResponse<DiaryResDto> diaries, int dailyCount) {
        return DiaryListResDto.builder()
                .diaries(diaries)
                .dailyCount(dailyCount)
                .build();
    }
}
