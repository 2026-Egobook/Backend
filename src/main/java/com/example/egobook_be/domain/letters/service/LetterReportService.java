package com.example.egobook_be.domain.letters.service;

import com.example.egobook_be.domain.letters.entity.PlazaLetter;
import com.example.egobook_be.domain.letters.entity.PlazaLetterReport;
import com.example.egobook_be.domain.letters.entity.ReplyReportReason;
import com.example.egobook_be.domain.letters.enums.LettersErrorCode;
import com.example.egobook_be.domain.letters.repository.PlazaLetterReportRepository;
import com.example.egobook_be.domain.letters.repository.PlazaLetterRepository;
import com.example.egobook_be.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class LetterReportService {

    private final PlazaLetterReportRepository letterReportRepository;
    private final PlazaLetterRepository plazaLetterRepository;

    @Transactional
    public void reportLetter(Long userId, Long letterId, ReplyReportReason reason, String description) {

        // 기타면 description 필수
        if (reason == ReplyReportReason.OTHER && (description == null || description.isBlank())) {
            throw new CustomException(LettersErrorCode.INVALID_REPORT_REASON);
        }

        // 이미 신고한 편지인지
        if (letterReportRepository.existsByLetter_LetterIdAndReporterId(letterId, userId)) {
            throw new CustomException(LettersErrorCode.ALREADY_REPORTED);
        }

        PlazaLetter letter = plazaLetterRepository.findById(letterId)
                .orElseThrow(() -> new CustomException(LettersErrorCode.LETTER_NOT_FOUND));

        // 권한: 받은 사람만 신고 가능 (보낸 사람은 신고 불가)
        if (letter.getReceiverId() == null || !userId.equals(letter.getReceiverId())) {
            throw new CustomException(LettersErrorCode.FORBIDDEN);
        }

        PlazaLetterReport report = PlazaLetterReport.builder()
                .letter(letter)
                .reporterId(userId)
                .senderId(letter.getSenderId())
                .reason(reason)
                .description(description)
                .createdAt(OffsetDateTime.now())
                .status(PlazaLetterReport.ReportStatus.PENDING)
                .build();

        letterReportRepository.save(report);

        // 3회 누적 시 편지 삭제
        long reportCount = letterReportRepository.countByLetter_LetterId(letterId);
        if (reportCount >= 3) {
            plazaLetterRepository.deleteById(letterId);
        }
    }
}

