package com.example.egobook_be.domain.psychology.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public record AdminKnowledgeResDto(
        Long id,
        String content,
        String source,
        @JsonProperty("created_at")
        LocalDateTime createdAt,
        @JsonProperty("deleted_at")
        LocalDateTime deletedAt
){}
