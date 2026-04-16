package com.example.egobook_be.domain.user.dto;

import com.example.egobook_be.domain.ego_room.enums.SendFailReason;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class AdminContentResDto {

    // ── B1. 일간 칭찬서 발송 현황 ────────────────────────────────────────────

    @Getter
    @Builder
    public static class DailyPraiseStatusRes {
        private SummaryWithDaily summary;
        private List<DailyStat> dailyStats;
        private List<PraiseFailLog> failLogs;
    }

    @Getter
    @Builder
    public static class SummaryWithDaily {
        private long scheduledCount;
        private long successCount;
        private long failCount;
    }

    @Getter
    @Builder
    public static class DailyStat {
        private LocalDate date;
        private long scheduledCount;
        private long successCount;
        private long failCount;
    }

    @Getter
    @Builder
    public static class PraiseFailLog {
        private Long failId;
        private Long userId;
        private LocalDateTime failedAt;
        private SendFailReason reason;
    }

    // ── B2. 주간 리포트 발송 현황 ────────────────────────────────────────────

    @Getter
    @Builder
    public static class WeeklyReportStatusRes {
        private SimpleSummary summary;
        private List<WeeklyFailLog> failLogs;
    }

    @Getter
    @Builder
    public static class SimpleSummary {
        private long scheduledCount;
        private long successCount;
        private long failCount;
    }

    @Getter
    @Builder
    public static class WeeklyFailLog {
        private Long failId;
        private Long userId;
        private LocalDateTime failedAt;
        private SendFailReason reason;
    }

    // ── B3. 편지 운영 현황 ───────────────────────────────────────────────────

    @Getter
    @Builder
    public static class LetterStatusRes {
        private LetterSummary summary;
        private List<LetterFailLog> failLogs;
    }

    @Getter
    @Builder
    public static class LetterSummary {
        private long sentCount;
        private long waitingCount;
        private long aiReplyCount;
        private long failCount;
    }

    @Getter
    @Builder
    public static class LetterFailLog {
        private Long logId;
        private Long letterId;
        private LocalDateTime failedAt;
        private String reason;
    }

    // ── B4. 나쁜말 차단 현황 ─────────────────────────────────────────────────

    @Getter
    @Builder
    public static class BadWordStatusRes {
        private BadWordSummary summary;
        private List<BlockedLog> blockedLogs;
    }

    @Getter
    @Builder
    public static class BadWordSummary {
        private long blockedCount;
        private double blockRate;
    }

    @Getter
    @Builder
    public static class BlockedLog {
        private Long blockId;
        private Long userId;
        private String type;
        private String originalText;
        private List<String> badWords;
        private double score;
        private LocalDateTime blockedAt;
    }

    // ── 재발송 공통 응답 ─────────────────────────────────────────────────────

    @Getter
    @Builder
    public static class ResendRes {
        private long successCount;
        private long failCount;
        private List<ResendResult> results;
    }

    @Getter
    @Builder
    public static class ResendResult {
        private Long failId;
        private String status;   // SUCCESS | FAIL
        private String reason;   // FAIL 시에만
    }
}