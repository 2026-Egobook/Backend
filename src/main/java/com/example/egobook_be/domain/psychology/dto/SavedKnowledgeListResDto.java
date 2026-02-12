package com.example.egobook_be.domain.psychology.dto;

import java.util.List;

public record SavedKnowledgeListResDto( List<SavedKnowledgeItemResDto> values, boolean hasNext, Long nextCursor ) {
    public record SavedKnowledgeItemResDto(
            Long knowledgeId,
            String title,
            String preview,
            String source,
            String savedAt ) {

    } }