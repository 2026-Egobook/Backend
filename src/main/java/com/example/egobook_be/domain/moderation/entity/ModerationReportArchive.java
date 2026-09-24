package com.example.egobook_be.domain.moderation.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "moderation_report_archive", uniqueConstraints =
@UniqueConstraint(name="uk_archive_source_report", columnNames={"source_type","source_report_id"}))
public class ModerationReportArchive {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Enumerated(EnumType.STRING) @Column(name="source_type", nullable=false, length=12)
    private ReportStrike.SourceType sourceType;
    @Column(name="source_report_id", nullable=false)
    private Long sourceReportId;
    @Column(name="target_user_id") private Long targetUserId;
    @Column(name="target_content_id") private Long targetContentId;
    @Column(name="reporter_id") private Long reporterId;
    @Column(name="reason", length=80) private String reason;
    @Column(name="description", length=2000) private String description;
    @Column(name="report_status", length=30) private String reportStatus;
    @Column(name="admin_memo", length=2000) private String adminMemo;
    @Column(name="original_created_at") private LocalDateTime originalCreatedAt;
    @Column(name="original_content", columnDefinition="TEXT") private String originalContent;

    public static ModerationReportArchive of(ReportStrike.SourceType sourceType, Long reportId,
                                             Long targetUserId, Long contentId, Long reporterId, String reason,
                                             String description, String status, String memo, LocalDateTime createdAt, String content) {
        ModerationReportArchive result = new ModerationReportArchive();
        result.sourceType = sourceType;
        result.sourceReportId = reportId;
        result.targetUserId = targetUserId;
        result.targetContentId = contentId;
        result.reporterId = reporterId;
        result.reason = reason;
        result.description = description;
        result.reportStatus = status;
        result.adminMemo = memo;
        result.originalCreatedAt = createdAt;
        result.originalContent = content;
        return result;
    }
}
