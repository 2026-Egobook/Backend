package com.example.egobook_be.domain.ads.repository;

import com.example.egobook_be.domain.ads.entity.AdRewardHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface AdRewardHistoryRepository extends JpaRepository<AdRewardHistory, Long> {
    /** 중복 지급 체크용 */
    boolean existsByTransactionId(String transactionId);

    /** 오늘 시청 횟수 조회 (인덱스(idx_user_created)를 타므로 매우 빠름) */
    @Query("SELECT COUNT(a) FROM AdRewardHistory a WHERE a.user.id = :userId AND a.createdAt BETWEEN :start AND :end")
    int countDailyAds(@Param("userId") Long userId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
