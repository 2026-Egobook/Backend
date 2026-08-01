package com.example.egobook_be.domain.coupon.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record CouponUseResDto(
        List<CouponRewardResDto> rewards
) {}
