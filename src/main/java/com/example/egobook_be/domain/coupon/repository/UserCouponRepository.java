package com.example.egobook_be.domain.coupon.repository;

import com.example.egobook_be.domain.coupon.entity.UserCoupon;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserCouponRepository extends JpaRepository<UserCoupon, Long> {

    /**
     * 해당 유저가 해당 쿠폰을 이미 사용했는지 확인
     */
    boolean existsByUserIdAndCouponId(Long userId, Long couponId);
}
