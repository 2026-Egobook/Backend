package com.example.egobook_be.domain.letters.repository;

import com.example.egobook_be.domain.letters.entity.BadWordBlockLog;
import com.example.egobook_be.domain.letters.enums.BlockType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface BadWordBlockLogRepository extends JpaRepository<BadWordBlockLog, Long> {

    List<BadWordBlockLog> findByBlockedAtBetweenOrderByBlockedAtDesc(
            LocalDateTime start, LocalDateTime end
    );

    List<BadWordBlockLog> findByBlockedAtBetweenAndTypeOrderByBlockedAtDesc(
            LocalDateTime start, LocalDateTime end, BlockType type
    );
}