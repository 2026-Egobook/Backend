package com.example.egobook_be.domain.letters.repository;

import com.example.egobook_be.domain.letters.entity.AiRequestCountLog;
import com.example.egobook_be.domain.letters.enums.BlockType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface AiRequestCountLogRepository extends JpaRepository<AiRequestCountLog, Long> {

    // 전체 유형 요청 수
    long countByRequestedAtBetween(LocalDateTime start, LocalDateTime end);

    // 특정 유형 요청 수
    long countByRequestedAtBetweenAndType(
            LocalDateTime start, LocalDateTime end, BlockType type
    );
}