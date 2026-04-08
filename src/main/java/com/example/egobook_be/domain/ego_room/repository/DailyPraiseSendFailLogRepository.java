package com.example.egobook_be.domain.ego_room.repository;

import com.example.egobook_be.domain.ego_room.entity.DailyPraiseSendFailLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface DailyPraiseSendFailLogRepository extends JpaRepository<DailyPraiseSendFailLog, Long> {

    List<DailyPraiseSendFailLog> findByTargetDateBetweenOrderByFailedAtDesc(
            LocalDate startDate, LocalDate endDate
    );

    @Query("""
        SELECT f.targetDate, COUNT(f)
        FROM DailyPraiseSendFailLog f
        WHERE f.targetDate BETWEEN :startDate AND :endDate
        GROUP BY f.targetDate
        ORDER BY f.targetDate ASC
    """)
    List<Object[]> countByDateRange(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}