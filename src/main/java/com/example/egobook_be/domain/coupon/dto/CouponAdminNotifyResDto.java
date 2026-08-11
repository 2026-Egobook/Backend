package com.example.egobook_be.domain.coupon.dto;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record CouponAdminNotifyResDto(
        Long couponId,
        boolean notified,
        LocalDateTime notifiedAt,
        int sentCount
) {}