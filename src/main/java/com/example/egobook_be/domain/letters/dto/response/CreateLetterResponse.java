package com.example.egobook_be.domain.letters.dto.response;

import com.example.egobook_be.domain.letters.entity.PlazaLetterMode;
import com.example.egobook_be.domain.letters.entity.PlazaLetterStatus;
import lombok.Builder;

import java.time.OffsetDateTime;

@Builder
public record CreateLetterResponse(
        Long letterId,
        Long threadId,
        PlazaLetterStatus status,
        PlazaLetterMode mode,
        String fromLabel,
        String backgroundColor,
        OffsetDateTime createdAt
) {}