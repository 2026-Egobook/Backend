package com.example.egobook_be.domain.letters.dto;

import com.example.egobook_be.domain.letters.domain.PlazaLetterMode;
import com.example.egobook_be.domain.letters.domain.PlazaLetterStatus;
import lombok.Builder;

import java.time.OffsetDateTime;

@Builder
public record CreateLetterResponse(
        Long letterId,
        Long threadId,
        PlazaLetterStatus status,
        PlazaLetterMode mode,
        OffsetDateTime createdAt
) {}