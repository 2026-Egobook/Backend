package com.example.egobook_be.domain.moderation.repository;

import com.example.egobook_be.domain.moderation.entity.ModerationReportArchive;
import com.example.egobook_be.domain.moderation.entity.ReportStrike;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ModerationReportArchiveRepository extends JpaRepository<ModerationReportArchive, Long> {
    Slice<ModerationReportArchive> findBySourceTypeOrderByOriginalCreatedAtDescIdDesc(
            ReportStrike.SourceType sourceType, Pageable pageable);
    List<ModerationReportArchive> findBySourceTypeAndTargetContentIdOrderByOriginalCreatedAtDescIdDesc(
            ReportStrike.SourceType sourceType, Long targetContentId);
    long countBySourceTypeAndTargetContentId(ReportStrike.SourceType sourceType, Long targetContentId);
}
