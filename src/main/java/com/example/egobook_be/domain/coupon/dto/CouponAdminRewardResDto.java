package com.example.egobook_be.domain.coupon.dto;

import com.example.egobook_be.domain.coupon.enums.CouponRewardType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "[관리자] 쿠폰 보상 응답 DTO")
public record CouponAdminRewardResDto(
        @Schema(description = "보상 PK", example = "23")
        Long rewardId,

        @Schema(description = "보상 타입", example = "INK")
        CouponRewardType rewardType,

        @Schema(description = "잉크 수량 (INK일 때만)", example = "12")
        Integer inkAmount,

        @Schema(description = "아이템 PK (ITEM일 때만)", example = "72")
        Long itemId
) {}