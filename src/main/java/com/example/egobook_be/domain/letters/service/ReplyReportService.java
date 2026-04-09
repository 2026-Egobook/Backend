package com.example.egobook_be.domain.letters.service;

import com.example.egobook_be.domain.letters.entity.*;
import com.example.egobook_be.domain.letters.repository.PlazaLetterReplyReportRepository;
import com.example.egobook_be.domain.letters.repository.PlazaLetterReplyRepository;
import com.example.egobook_be.domain.letters.repository.PlazaLetterRepository;
import com.example.egobook_be.global.enums.ReportReason;
import com.example.egobook_be.global.enums.ReportStatus;
import com.example.egobook_be.global.exception.CustomException;
import com.example.egobook_be.domain.letters.enums.LettersErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ReplyReportService {

    private final PlazaLetterReplyReportRepository replyReportRepository;
    private final PlazaLetterReplyRepository plazaLetterReplyRepository;
    private final PlazaLetterRepository plazaLetterRepository;

    @Transactional
    public void reportReply(Long userId, Long replyId, ReportReason reason, String description) {
        // "기타"일 경우 사유 입력 값이 있어야 함
        if (reason == ReportReason.OTHER && (description == null || description.isBlank())) {
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
                .status(ReportStatus.PENDING)
                .build();

        replyReportRepository.save(report);

        // 신고된 답장의 신고 횟수 확인
        long reportCount = replyReportRepository.countByReply_ReplyId(replyId);

        // 3회 신고가 누적되었으면 해당 답장 삭제 및 신고 DB로 이동
        if (reportCount >= 3) {
            // 3회 누적 신고된 답장 삭제 및 신고 DB로 이동
            moveReplyToReportDbAndDelete(replyId);
        }
    }

    // 신고 횟수 3회 누적 시 답장 삭제 및 신고 DB로 이동
    private void moveReplyToReportDbAndDelete(Long replyId) {
        // 신고 DB로 이동하고 답장 삭제 처리
        replyReportRepository.moveReplyToReportDbAndDelete(replyId, PlazaLetterReply.ReplyStatus.DELETED);

        // 해당 답장 삭제
        plazaLetterReplyRepository.deleteById(replyId);
    }


}

