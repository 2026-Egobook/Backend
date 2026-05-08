package com.example.egobook_be.domain.user.repository;

import com.example.egobook_be.domain.user.entity.AbilityLog;
import com.example.egobook_be.domain.user.entity.AbilityLogReason;
import com.example.egobook_be.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface AbilityLogRepository extends JpaRepository<AbilityLog, Long> {

    /**
     * 사용자가 특정 능력치 보상을 오늘 이미 획득했는지 확인한다.
     * @param user : 보상 획득 여부를 확인할 사용자
     * @param reason : 능력치 보상 사유
     * @param start : 조회 시작 시각
     * @param end : 조회 종료 시각
     * @return : 오늘 능력치 보상 획득 여부
     */
    boolean existsByUserAndReasonAndCreatedAtBetween(
            User user,
            AbilityLogReason reason,
            LocalDateTime start,
            LocalDateTime end
    );
}
