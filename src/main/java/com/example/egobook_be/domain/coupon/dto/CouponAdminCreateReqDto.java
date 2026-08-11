package com.example.egobook_be.domain.coupon.dto;

import com.example.egobook_be.domain.coupon.enums.CouponTargetType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;
import java.util.List;

public record CouponAdminCreateReqDto(

        @Schema(description = "쿠폰 코드", example = "XK7QPT2A")
        @NotBlank(message = "쿠폰 코드를 입력해주세요")
        @Size(max = 20, message = "쿠폰 코드는 20자 이하여야 합니다")
        @Pattern(regexp = "^[A-Za-z0-9-]+$", message = "쿠폰 코드는 영문/숫자/하이픈만 사용할 수 있습니다")
        String code,

        @Schema(description = "대상", example = "INDIVIDUAL")
        @NotNull(message = "대상은 필수입니다")
        CouponTargetType targetType,

        @Schema(description = "계정 고유 코드 (대상이 INDIVIDUAL일 때 필수)", example = "A1B2C3D4")
        @Size(max = 12, message = "계정 고유 코드는 12자 이하여야 합니다")
        String targetAccountCode,

        @Schema(description = "보상 목록 (배열 순서가 sortOrder로 저장됨)")
        @NotEmpty(message = "보상은 1개 이상 등록해야 합니다")
        @Valid
        List<CouponAdminRewardReqDto> rewards,

        @Schema(description = "만료 일시", example = "2026-03-24T14:00:00")
        @NotNull(message = "만료 일시는 필수입니다")
        @Future(message = "만료 일시는 현재 이후여야 합니다")
        LocalDateTime expiresAt
) {}