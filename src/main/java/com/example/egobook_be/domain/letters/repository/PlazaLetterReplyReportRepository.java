package com.example.egobook_be.domain.letters.repository;

import com.example.egobook_be.domain.letters.entity.PlazaLetterReplyReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlazaLetterReplyReportRepository extends JpaRepository<PlazaLetterReplyReport, Long> {

    boolean existsByReply_ReplyIdAndReporterId(Long replyId, Long reporterId);
}
