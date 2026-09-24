package com.example.egobook_be.domain.moderation.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "moderation_report_strike", uniqueConstraints =
@UniqueConstraint(name = "uk_strike_source_report", columnNames = {"source_type", "source_report_id"}),
        indexes = {@Index(name = "idx_strike_author_kind", columnList = "target_user_id,source_type"),
                @Index(name = "idx_strike_answer", columnList = "answer_id")})
public class ReportStrike {
    public enum SourceType { LETTER, REPLY, ANSWER }
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 12)
    private SourceType sourceType;
    @Column(name = "source_report_id", nullable = false)
    private Long sourceReportId;
    @Column(name = "target_user_id", nullable = false)
    private Long targetUserId;
    @Column(name = "answer_id")
    private Long answerId;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public static ReportStrike of(SourceType sourceType, Long sourceReportId, Long targetUserId, Long answerId) {
        ReportStrike result = new ReportStrike();
        result.sourceType = sourceType;
        result.sourceReportId = sourceReportId;
        result.targetUserId = targetUserId;
        result.answerId = answerId;
        result.createdAt = LocalDateTime.now();
        return result;
    }
}
