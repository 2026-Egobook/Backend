package com.example.egobook_be.domain.coupon.repository;

import com.example.egobook_be.domain.coupon.entity.Coupon;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CouponRepository extends JpaRepository<Coupon, Long> {

    /**
     * 쿠폰 코드로 쿠폰 조회 (보상 목록 fetch join)
     */
    @Query("select c from Coupon c left join fetch c.rewards r where c.code = :code order by r.sortOrder asc")
    Optional<Coupon> findByCodeWithRewards(@Param("code") String code);

    // Admin

    boolean existsByCode(String code);

    /** 수정 시 자기 자신을 제외한 코드 중복 검증 */
    boolean existsByCodeAndIdNot(String code, Long id);

    /**
     * 전체 쿠폰의 ID를 등록 최신순으로 조회한다. (만료 여부 무관)
     * - 컬렉션 fetch join과 페이징을 함께 쓰면 Hibernate가 전체 행을 메모리에 적재하므로 ID 조회와 분리한다.
     * - createdAt이 같을 때 페이지 간 순서가 흔들리지 않도록 id를 보조 정렬 기준으로 둔다.
     */
    @Query("select c.id from Coupon c order by c.createdAt desc, c.id desc")
    Slice<Long> findCouponIds(Pageable pageable);

    /** ID 목록으로 쿠폰 + 보상을 한 번에 조회한다. */
    @Query("select distinct c from Coupon c left join fetch c.rewards r where c.id in :ids")
    List<Coupon> findAllWithRewardsByIdIn(@Param("ids") List<Long> ids);

    @Query("select c from Coupon c left join fetch c.rewards r where c.id = :couponId")
    Optional<Coupon> findByIdWithRewards(@Param("couponId") Long couponId);

    /**
     * 아직 알림을 보내지 않은 쿠폰만 전송 완료로 표시한다.
     * - 동시에 두 요청이 들어와도 UPDATE가 성공한 한쪽만 실제 전송을 진행하도록 선점한다.
     * @return : 갱신된 행 수 (0이면 이미 다른 요청이 선점함)
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Coupon c set c.notifiedAt = :now where c.id = :couponId and c.notifiedAt is null")
    int markNotifiedIfNotYet(@Param("couponId") Long couponId, @Param("now") LocalDateTime now);
}