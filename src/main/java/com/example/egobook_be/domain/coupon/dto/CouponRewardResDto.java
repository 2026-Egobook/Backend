package com.example.egobook_be.domain.coupon.dto;

import com.example.egobook_be.domain.coupon.enums.CouponRewardType;
import lombok.Builder;

@Builder
public record CouponRewardResDto(
        CouponRewardType rewardType,
        Integer inkAmount,
        Long itemId,
        String itemName,
        String itemImageUrl
) {}
