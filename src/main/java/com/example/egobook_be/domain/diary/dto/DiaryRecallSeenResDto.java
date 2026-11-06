package com.example.egobook_be.domain.diary.dto;

import lombok.Builder;

import java.time.LocalDate;

@Builder
public record DiaryRecallSeenResDto(
        LocalDate recalledAt,
        LocalDate nextRecallDate
) {}