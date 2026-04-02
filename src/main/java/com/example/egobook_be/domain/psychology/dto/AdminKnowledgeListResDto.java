package com.example.egobook_be.domain.psychology.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.List;

public record AdminKnowledgeListResDto(
        List<AdminKnowledgeItemResDto> list,
        Boolean hasNext
) {
    public record AdminKnowledgeItemResDto(
            Long id,
            String content,
            String source,
            @JsonProperty("created_at")
            LocalDateTime createdAt,
            @JsonProperty("deleted_at")
            LocalDateTime deletedAt
    ) {}
}