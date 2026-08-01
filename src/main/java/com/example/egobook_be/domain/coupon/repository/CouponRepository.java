package com.example.egobook_be.domain.coupon.repository;

import com.example.egobook_be.domain.coupon.entity.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CouponRepository extends JpaRepository<Coupon, Long> {

    /**
     * 쿠폰 코드로 쿠폰 조회 (보상 목록 fetch join)
     */
    @Query("select c from Coupon c left join fetch c.rewards r where c.code = :code order by r.sortOrder asc")
    Optional<Coupon> findByCodeWithRewards(@Param("code") String code);
}
