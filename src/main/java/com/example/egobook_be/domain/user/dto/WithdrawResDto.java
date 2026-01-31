package com.example.egobook_be.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record WithdrawResDto(
        @Schema(description = "사용자가 회원 탈퇴를 수행한 시각", defaultValue = "2026-01-01")
        LocalDateTime deletedAt,
        @Schema(description = "사용자 데이터 완전 삭제까지 남은 일수", defaultValue = "7")
        Integer graceDays
) {
}
