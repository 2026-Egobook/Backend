package com.example.egobook_be.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record WithdrawReqDto(
        @Schema(description = "회원 탈퇴 여부", defaultValue = "true")
        @NotNull
        Boolean confirm
) {
}
