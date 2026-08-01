package com.example.egobook_be.domain.coupon.entity;

import com.example.egobook_be.domain.coupon.enums.CouponRewardType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "coupon_reward")
public class CouponReward {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id", nullable = false)
    private Coupon coupon;

    @Enumerated(EnumType.STRING)
    @Column(name = "reward_type", nullable = false, length = 20)
    private CouponRewardType rewardType;

    @Column(name = "ink_amount")
    private Integer inkAmount;

    @Column(name = "item_id")
    private Long itemId;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;
}
