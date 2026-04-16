package com.example.egobook_be.domain.letters.entity;

import com.example.egobook_be.domain.letters.enums.BlockType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 나쁜말 AI 차단 이력
 * WordClientService.shouldBlock() 이 true 일 때 기록
 * 차단 기준: percentage >= 80.0
 */
@Entity
@Table(name = "bad_word_block_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class BadWordBlockLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    /** 차단 유형 (LETTER, REPLY, PRAISE) */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BlockType type;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String originalText;

    /** 감지된 나쁜말 목록 */
    @ElementCollection
    @CollectionTable(
            name = "bad_word_block_log_words",
            joinColumns = @JoinColumn(name = "block_log_id")
    )
    @Column(name = "word")
    private List<String> badWords;

    /** 차단 점수 (0.0 ~ 1.0) */
    @Column(nullable = false)
    private double score;

    @Column(nullable = false)
    private LocalDateTime blockedAt;
}
