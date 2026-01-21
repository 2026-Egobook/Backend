package com.example.egobook_be.domain.diary.dto;

import com.example.egobook_be.global.response.SliceResponse;
import lombok.Builder;


@Builder
public record DiaryListResDto(
        int dailyCount,
        SliceResponse<DiaryResDto> diaries
) {}
