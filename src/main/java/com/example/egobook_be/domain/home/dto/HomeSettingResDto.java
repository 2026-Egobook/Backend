package com.example.egobook_be.domain.home.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record HomeSettingResDto(
        @Schema(description = "사용자 고유 계정 ID (Account Code)", example = "EGO-123456")
        String accountCode
) {
}
