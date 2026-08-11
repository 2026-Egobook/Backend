package com.example.egobook_be.domain.coupon.mapper;

import com.example.egobook_be.domain.coupon.dto.CouponAdminNotifyResDto;
import com.example.egobook_be.domain.coupon.dto.CouponAdminResDto;
import com.example.egobook_be.domain.coupon.dto.CouponAdminRewardResDto;
import com.example.egobook_be.domain.coupon.entity.Coupon;
import com.example.egobook_be.domain.coupon.entity.CouponReward;
import org.springframework.stereotype.Component;

import java.util.Comparator;

@Component
public class CouponMapper {

    /**
     * Coupon Entity -> CouponAdminResDto 변환
     * @param coupon : rewards가 fetch join된 Coupon Entity
     */
    public CouponAdminResDto toCouponAdminResDto(Coupon coupon) {
        return CouponAdminResDto.builder()
                .couponId(coupon.getId())
                .code(coupon.getCode())
                .targetType(coupon.getTargetType())
                .targetAccountCode(coupon.getTargetAccountCode())
                .rewards(coupon.getRewards().stream()
                        .sorted(Comparator.comparing(CouponReward::getSortOrder))
                        .map(this::toCouponAdminRewardResDto)
                        .toList())
                .expiresAt(coupon.getExpiresAt())
                .notified(coupon.isNotified())
                .notifiedAt(coupon.getNotifiedAt())
                .createdAt(coupon.getCreatedAt())
                .build();
    }

    private CouponAdminRewardResDto toCouponAdminRewardResDto(CouponReward reward) {
        return CouponAdminRewardResDto.builder()
                .rewardId(reward.getId())
                .rewardType(reward.getRewardType())
                .inkAmount(reward.getInkAmount())
                .itemId(reward.getItemId())
                .build();
    }

    public CouponAdminNotifyResDto toCouponAdminNotifyResDto(Coupon coupon, int sentCount) {
        return CouponAdminNotifyResDto.builder()
                .couponId(coupon.getId())
                .notified(coupon.isNotified())
                .notifiedAt(coupon.getNotifiedAt())
                .sentCount(sentCount)
                .build();
    }
}