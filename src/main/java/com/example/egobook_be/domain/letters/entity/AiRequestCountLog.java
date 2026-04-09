package com.example.egobook_be.domain.letters.entity;

import com.example.egobook_be.domain.letters.enums.BlockType;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * 나쁜말 AI 전체 요청 수 카운트
 * blockRate = blockedCount / totalCount * 100 계산용
 */
@Entity
@Table(name = "ai_request_count_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class AiRequestCountLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BlockType type;  // LETTER, REPLY, PRAISE

    @Column(nullable = false)
    private LocalDateTime requestedAt;
}
