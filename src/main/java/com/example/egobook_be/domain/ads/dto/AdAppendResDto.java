package com.example.egobook_be.domain.ads.dto;

import com.example.egobook_be.domain.ads.enums.AdStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record AdAppendResDto(
        @Schema(description = "광고 PK", example = "10")
        Long adId,
        @Schema(description = "광고 제목", example = "전설의 검 RPG")
        String title,
        @Schema(description = "광고 상태", example = "PENDING")
        AdStatus status
) {
}
