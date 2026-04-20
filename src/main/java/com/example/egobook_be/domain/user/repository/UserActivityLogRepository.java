package com.example.egobook_be.domain.user.repository;

import com.example.egobook_be.domain.user.entity.UserActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface UserActivityLogRepository extends JpaRepository<UserActivityLog, Long> {

    @Query(value = """
    SELECT COUNT(DISTINCT a.user_id)
    FROM user_activity_log a
    JOIN user u ON u.id = a.user_id
    WHERE a.active_date = DATE(DATE_ADD(u.created_at, INTERVAL :days DAY))
    """, nativeQuery = true)
    Long countRetainedUserOnDay(@Param("days") int days);

    @Query(value = """
    SELECT active_date AS date, COUNT(DISTINCT user_id) AS count
    FROM user_activity_log
    WHERE active_date >= :startDate
      AND active_date <= :endDate
    GROUP BY active_date
    ORDER BY active_date
    """, nativeQuery = true)
    List<AuCount> countDau(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query(value = """
    WITH RECURSIVE date_series AS (
        SELECT :startDate AS dt
        UNION ALL
        SELECT dt + INTERVAL 1 DAY
        FROM date_series
        WHERE dt < :endDate
    )
    SELECT
        d.dt        AS date,
        COUNT(DISTINCT a.user_id) AS count
    FROM date_series d
    LEFT JOIN user_activity_log a
        ON a.active_date BETWEEN DATE_SUB(d.dt, INTERVAL 29 DAY) AND d.dt
    GROUP BY d.dt
    ORDER BY d.dt
    """, nativeQuery = true)
    List<AuCount> countMau(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );



    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
    INSERT IGNORE INTO user_activity_log (user_id, active_date)
    VALUES (:userId, :activeDate)
    """, nativeQuery = true)
    void insertIgnore(@Param("userId") Long userId, @Param("activeDate") LocalDate activeDate);

    interface AuCount {
        LocalDate getDate();
        Long getCount();
    }
}
