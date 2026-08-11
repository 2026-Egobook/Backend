package com.example.egobook_be.domain.coupon.dto;

import com.example.egobook_be.domain.coupon.enums.CouponTargetType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Builder
@Schema(description = "[관리자] 쿠폰 응답 DTO")
public record CouponAdminResDto(
        Long couponId,
        String code,
        CouponTargetType targetType,
        String targetAccountCode,
        List<CouponAdminRewardResDto> rewards,
        LocalDateTime expiresAt,
        boolean notified,
        LocalDateTime notifiedAt,
        LocalDateTime createdAt
) {}