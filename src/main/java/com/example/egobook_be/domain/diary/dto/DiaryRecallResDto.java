package com.example.egobook_be.domain.diary.dto;

import lombok.Builder;

@Builder
public record DiaryRecallResDto(
        boolean available,
        DiaryResDto diary
) {}