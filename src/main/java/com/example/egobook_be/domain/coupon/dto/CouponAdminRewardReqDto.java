package com.example.egobook_be.domain.coupon.dto;

import com.example.egobook_be.domain.coupon.enums.CouponRewardType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CouponAdminRewardReqDto(

        @Schema(description = "보상 타입", example = "INK")
        @NotNull(message = "보상 타입은 필수입니다")
        CouponRewardType rewardType,

        @Schema(description = "잉크 수량 (rewardType이 INK일 때 필수)", example = "12")
        @Positive(message = "잉크 수량은 1 이상이어야 합니다")
        Integer inkAmount,

        @Schema(description = "아이템 PK (rewardType이 ITEM일 때 필수)", example = "72")
        Long itemId
) {}