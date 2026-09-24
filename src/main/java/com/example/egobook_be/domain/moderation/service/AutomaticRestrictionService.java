package com.example.egobook_be.domain.moderation.service;

import com.example.egobook_be.domain.moderation.entity.ReportStrike;
import com.example.egobook_be.domain.moderation.repository.ModerationUserLockRepository;
import com.example.egobook_be.domain.moderation.repository.ReportStrikeRepository;
import com.example.egobook_be.domain.restriction.entity.Restriction;
import com.example.egobook_be.domain.restriction.enums.RestrictionDomainType;
import com.example.egobook_be.domain.restriction.enums.RestrictionStatus;
import com.example.egobook_be.domain.restriction.repository.RestrictionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class AutomaticRestrictionService {
    private final ReportStrikeRepository strikes;
    private final ModerationUserLockRepository users;
    private final RestrictionRepository restrictions;
    private final ModerationContentCleanupService cleanup;

    // All callers hold one outer write transaction. Lock must be acquired before saving the report.
    // Every report path must acquire this SAME user lock to serialize counts across domains.
    @Transactional(propagation = Propagation.MANDATORY)
    public void lockAuthor(Long authorId) {
        users.lockById(authorId).orElseThrow(() -> new IllegalArgumentException("작성자가 없습니다: " + authorId));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void onReportSaved(ReportStrike.SourceType source, Long reportId, Long authorId, Long answerId) {
        strikes.saveAndFlush(ReportStrike.of(source, reportId, authorId, answerId));
        long count = source == ReportStrike.SourceType.ANSWER
                ? strikes.countBySourceTypeAndAnswerId(ReportStrike.SourceType.ANSWER, answerId)
                : strikes.countByTargetUserIdAndSourceTypeIn(authorId,
                java.util.List.of(ReportStrike.SourceType.LETTER, ReportStrike.SourceType.REPLY));

        // Lifetime threshold exactly 3: cancellation does not re-trigger old reports.
        if (count != 3) return;
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
        // Treat one automatic incident as covering BOTH features.
        boolean letterActive = restrictions.existsByUserIdAndDomainTypeAndStatusAndRestrictionUntilAfter(
                authorId, RestrictionDomainType.LETTER, RestrictionStatus.ACTIVE, now);
        boolean answerActive = restrictions.existsByUserIdAndDomainTypeAndStatusAndRestrictionUntilAfter(
                authorId, RestrictionDomainType.QUESTION_ANSWER, RestrictionStatus.ACTIVE, now);
        String description = source == ReportStrike.SourceType.ANSWER
                ? "동일 질문 답변 신고 3건 접수로 자동 제재"
                : "작성자 편지·답장 신고 합계 3건 접수로 자동 제재";
        if (!letterActive) restrictions.save(Restriction.createAutomatic(authorId, RestrictionDomainType.LETTER, description));
        if (!answerActive) restrictions.save(Restriction.createAutomatic(authorId, RestrictionDomainType.QUESTION_ANSWER, description));
        // Archive moderation evidence and delete all authored content in the SAME transaction.
        cleanup.archiveAndDeleteAllAuthoredContent(authorId);
    }
}
