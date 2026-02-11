package com.example.egobook_be.domain.home.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record HomeSettingResDto(
        @Schema(description = "사용자 고유 계정 ID (Account Code)", example = "EGO-123456")
        String accountCode,
        @Schema(description = "사용자 Email", example = "example@google.com")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String userEmail
) {
}
