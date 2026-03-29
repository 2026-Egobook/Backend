package com.example.egobook_be.domain.letters.dto.response;

import com.example.egobook_be.domain.letters.entity.PlazaLetterMode;
import com.example.egobook_be.domain.letters.entity.PlazaLetterStatus;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record CreateLetterResponse(
        Long letterId,
        Long threadId,
        PlazaLetterStatus status,
        PlazaLetterMode mode,
        String fromLabel,
        String backgroundColor,
        LocalDateTime createdAt,
        String backgroundImageUrl
        ) {}