package com.example.egobook_be.domain.letters.repository;

import com.example.egobook_be.domain.letters.entity.LetterSendFailLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface LetterSendFailLogRepository extends JpaRepository<LetterSendFailLog, Long> {

    List<LetterSendFailLog> findByFailedAtBetweenOrderByFailedAtDesc(
            LocalDateTime start, LocalDateTime end
    );
}
