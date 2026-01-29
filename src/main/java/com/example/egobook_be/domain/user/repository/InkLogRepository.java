package com.example.egobook_be.domain.user.repository;

import com.example.egobook_be.domain.user.entity.InkLog;
import com.example.egobook_be.domain.user.entity.InkLogType;
import com.example.egobook_be.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;

@Repository
public interface InkLogRepository extends JpaRepository<InkLog, Long> {
    boolean existsByUserAndReasonAndCreatedAtAfter(User user, InkLogType reason, LocalDateTime startOfDay);
}