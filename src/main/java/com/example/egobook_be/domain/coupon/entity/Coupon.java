package com.example.egobook_be.domain.coupon.entity;

import com.example.egobook_be.domain.coupon.enums.CouponTargetType;
import com.example.egobook_be.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "coupon")
public class Coupon extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 20)
    private CouponTargetType targetType;

    @Column(name = "target_account_code", length = 12)
    private String targetAccountCode;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "notified_at")
    private LocalDateTime notifiedAt;

    @OneToMany(mappedBy = "coupon", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CouponReward> rewards = new ArrayList<>();

    /**
     * 쿠폰 기본 정보를 수정한다. (보상은 replaceRewards로 별도 교체)
     */
    public void update(String code, CouponTargetType targetType, String targetAccountCode, LocalDateTime expiresAt) {
        this.code = code;
        this.targetType = targetType;
        this.targetAccountCode = targetAccountCode;
        this.expiresAt = expiresAt;
    }

    /**
     * 보상 목록을 통째로 교체한다.
     * - 수정 팝업에서 '-' 버튼으로 제거된 항목은 요청 배열에서 빠져오므로 orphanRemoval로 삭제된다.
     */
    public void replaceRewards(List<CouponReward> newRewards) {
        this.rewards.clear();
        newRewards.forEach(this::addReward);
    }

    public void addReward(CouponReward reward) {
        this.rewards.add(reward);
        reward.assignCoupon(this);
    }

    public void markNotified() {
        this.notifiedAt = LocalDateTime.now();
    }

    public boolean isNotified() {
        return this.notifiedAt != null;
    }

    /** 만료 여부를 판단한다. 만료 일시 이전까지 유효하다. */
    public boolean isExpired(LocalDateTime now) {
        return now.isAfter(this.expiresAt);
    }
}