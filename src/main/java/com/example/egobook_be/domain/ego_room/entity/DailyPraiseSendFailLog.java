package com.example.egobook_be.domain.ego_room.entity;

import com.example.egobook_be.domain.ego_room.enums.SendFailReason;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * AI 일간 칭찬서 발송 실패 로그
 * AiScheduler에서 createDailyPraise() 실패 시 기록
 */
@Entity
@Table(name = "daily_praise_send_fail_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class DailyPraiseSendFailLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    /** 발송 대상 날짜 */
    @Column(nullable = false)
    private LocalDate targetDate;

    /** 실패 사유 (FCM_TOKEN_NOT_FOUND, AI_RESPONSE_TIMEOUT 등) */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SendFailReason reason;

    @Column(nullable = false)
    private LocalDateTime failedAt;

    /** 재발송 완료 여부 */
    @Column(nullable = false)
    @Builder.Default
    private boolean resent = false;

    @Column
    private LocalDateTime resentAt;

    public void markResent() {
        this.resent = true;
        this.resentAt = LocalDateTime.now();
    }
}