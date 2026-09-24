package com.example.egobook_be.domain.moderation.repository;
import com.example.egobook_be.domain.moderation.entity.ModerationReportArchive;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ModerationReportArchiveRepository extends JpaRepository<ModerationReportArchive, Long> {}
