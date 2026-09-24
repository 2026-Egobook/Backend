package com.example.egobook_be.domain.moderation.repository;

import com.example.egobook_be.domain.moderation.entity.ReportStrike;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Collection;

public interface ReportStrikeRepository extends JpaRepository<ReportStrike, Long> {
    long countByTargetUserIdAndSourceTypeIn(Long userId, Collection<ReportStrike.SourceType> types);
    long countBySourceTypeAndAnswerId(ReportStrike.SourceType type, Long answerId);
}
