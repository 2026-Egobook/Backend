package com.example.egobook_be.domain.user.repository;

import com.example.egobook_be.domain.user.entity.WithdrawReason;
import com.example.egobook_be.domain.user.enums.WithdrawReasonType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface WithdrawReasonRepository extends JpaRepository<WithdrawReason, Long> {
    boolean existsByUserId(Long userId);

    @Query("""
    SELECT w.withdrawReasonType AS type, COUNT(w) AS count
    FROM WithdrawReason w
    WHERE w.createdAt >= :startDateTime
      AND w.createdAt < :endDateTime
    GROUP BY w.withdrawReasonType
    """)
    List<WithDrawReasonCount> countReasons(
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime
    );

    @Query("""
    SELECT w.text
    FROM WithdrawReason w
    WHERE w.createdAt >= :startDateTime
      AND w.createdAt < :endDateTime
      AND w.withdrawReasonType = com.example.egobook_be.domain.user.enums.WithdrawReasonType.OTHER
      AND w.text IS NOT NULL
    """)
    List<String> findOtherTexts(
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime
    );

    Long countByCreatedAtGreaterThanEqualAndCreatedAtBefore(LocalDateTime createdAtIsGreaterThan, LocalDateTime createdAtBefore);

    interface WithDrawReasonCount {
        WithdrawReasonType getType();
        Long getCount();
    }
}
