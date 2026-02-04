package com.example.egobook_be.domain.letters.service;

import com.example.egobook_be.domain.letters.entity.*;
import com.example.egobook_be.domain.letters.repository.PlazaLetterReplyReportRepository;
import com.example.egobook_be.domain.letters.repository.PlazaLetterReplyRepository;
import com.example.egobook_be.domain.letters.repository.PlazaLetterRepository;
import com.example.egobook_be.global.exception.CustomException;
import com.example.egobook_be.domain.letters.enums.LettersErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class ReplyReportService {

    private final PlazaLetterReplyReportRepository replyReportRepository;
    private final PlazaLetterReplyRepository plazaLetterReplyRepository;
    private final PlazaLetterRepository plazaLetterRepository;

    @Transactional
    public void reportReply(Long userId, Long replyId, ReplyReportReason reason, String description) {
        // "기타"일 경우 사유 입력 값이 있어야 함
        if (reason == ReplyReportReason.OTHER && (description == null || description.isBlank())) {
            throw new CustomException(LettersErrorCode.INVALID_REPORT_REASON);
        }

        // 이미 신고한 답장인지 체크
        boolean alreadyReported = replyReportRepository.existsByReply_ReplyIdAndReporterId(replyId, userId);
        if (alreadyReported) {
            throw new CustomException(LettersErrorCode.ALREADY_REPORTED);
        }

        // 신고 대상 답장과 편지를 가져오기
        PlazaLetterReply reply = plazaLetterReplyRepository.findByIdWithLetter(replyId)
                .orElseThrow(() -> new CustomException(LettersErrorCode.LETTER_NOT_FOUND));

        PlazaLetter letter = reply.getLetter();

        if (userId.equals(letter.getReceiverId())) {
            throw new CustomException(LettersErrorCode.FORBIDDEN);
        }

        // 신고 저장
        PlazaLetterReplyReport report = PlazaLetterReplyReport.builder()
                .reply(reply)
                .letter(letter)
                .reporterId(userId)
                .replierId(reply.getReplierId())
                .reason(reason)
                .description(description)
                .createdAt(OffsetDateTime.now())
                .status(PlazaLetterReplyReport.ReportStatus.PENDING)
                .build();

        replyReportRepository.save(report);
    }
}

