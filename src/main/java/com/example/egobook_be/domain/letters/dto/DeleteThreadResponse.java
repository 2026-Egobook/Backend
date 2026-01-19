package com.example.egobook_be.domain.letters.dto;

import lombok.Builder;

@Builder
public record DeleteThreadResponse(
        Long threadId,
        boolean deleted
) {}
