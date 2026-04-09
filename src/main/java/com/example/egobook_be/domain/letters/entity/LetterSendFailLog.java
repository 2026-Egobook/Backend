package com.example.egobook_be.domain.letters.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 편지 발송 실패 로그
 * 수신자 배정 실패 등 편지 운영 중 발생한 실패 기록
 */
@Entity
@Table(name = "letter_send_fail_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class LetterSendFailLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long letterId;

    /** 실패 사유 (NO_RECEIVER_AVAILABLE, FCM_SEND_FAIL 등) */
    @Column(nullable = false, length = 100)
    private String reason;

    @Column(nullable = false)
    private LocalDateTime failedAt;
}