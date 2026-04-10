package com.example.egobook_be.domain.ego_room.repository;

import com.example.egobook_be.domain.ego_room.entity.DailyPraise;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyPraiseRepository extends JpaRepository<DailyPraise, Long> {

    Slice<DailyPraise> findAllByUserId(Long userId, Pageable pageable);

    Optional<DailyPraise> findByUserIdAndPraiseDate(Long userId, LocalDate praiseDate);

    // 날짜별 발송 성공 건수 집계 (관리자 API용)
    @Query("""
        SELECT dp.praiseDate, COUNT(dp)
        FROM DailyPraise dp
        WHERE dp.praiseDate BETWEEN :startDate AND :endDate
        GROUP BY dp.praiseDate
        ORDER BY dp.praiseDate ASC
    """)
    List<Object[]> countSuccessByDateRange(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    long countByPraiseDateBetween(LocalDate startDate, LocalDate endDate);

}