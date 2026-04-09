package com.example.egobook_be.domain.ego_room.entity;

import com.example.egobook_be.domain.ego_room.enums.SendFailReason;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * AI 주간 리포트 발송 실패 로그
 * AiScheduler에서 createWeeklyAnalysis() 실패 시 기록
 */
@Entity
@Table(name = "weekly_report_send_fail_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class WeeklyReportSendFailLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    /** 해당 주의 월요일 날짜 */
    @Column(nullable = false)
    private LocalDate weekStartDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SendFailReason reason;

    @Column(nullable = false)
    private LocalDateTime failedAt;

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