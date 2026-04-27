package com.example.egobook_be.domain.stat.dto;

import lombok.Builder;

@Builder
public record AdminRetentionResDto (
        Double day7RetentionRate,
        Double day30RetentionRate
) {}
