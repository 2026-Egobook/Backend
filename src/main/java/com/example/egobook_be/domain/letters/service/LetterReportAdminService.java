package com.example.egobook_be.domain.letters.service;

import com.example.egobook_be.domain.letters.dto.response.PlazaLetterReportAdminResDto;
import com.example.egobook_be.domain.letters.dto.response.PlazaLetterReplyReportAdminResDto;
import com.example.egobook_be.domain.letters.entity.PlazaLetterReport;
import com.example.egobook_be.domain.letters.entity.PlazaLetterReplyReport;
import com.example.egobook_be.domain.letters.enums.LettersErrorCode;
import com.example.egobook_be.domain.letters.repository.PlazaLetterReportRepository;
import com.example.egobook_be.domain.letters.repository.PlazaLetterReplyReportRepository;
import com.example.egobook_be.global.exception.CustomException;
import com.example.egobook_be.global.response.SliceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LetterReportAdminService {

    private final PlazaLetterReportRepository letterReportRepository;
    private final PlazaLetterReplyReportRepository replyReportRepository;

    public SliceResponse<PlazaLetterReportAdminResDto> getReportedLetters(int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 50);
        Pageable pageable = PageRequest.of(safePage - 1, safeSize);

        Slice<PlazaLetterReport> slice = letterReportRepository.findAllWithLetter(pageable);

        return SliceResponse.of(slice, report -> new PlazaLetterReportAdminResDto(
                report.getReportId(),
                report.getLetter().getLetterId(),
                report.getLetter().getContent(),
                report.getReporterId(),
                report.getReason(),
                report.getDescription(),
                report.getStatus(),
                report.getCreatedAt()
        ));
    }

    public SliceResponse<PlazaLetterReplyReportAdminResDto> getReportedReplies(int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 50);
        Pageable pageable = PageRequest.of(safePage - 1, safeSize);

        Slice<PlazaLetterReplyReport> slice = replyReportRepository.findAllWithReply(pageable);

        return SliceResponse.of(slice, report -> new PlazaLetterReplyReportAdminResDto(
                report.getReportId(),
                report.getReply().getReplyId(),
                report.getReply().getContent(),
                report.getReporterId(),
                report.getReason(),
                report.getDescription(),
                report.getStatus(),
                report.getCreatedAt()
        ));
    }

    //상세 조회
    public PlazaLetterReportAdminResDto getReportedLetterDetail(Long reportId) {
        PlazaLetterReport report = letterReportRepository.findByIdWithLetter(reportId)
                .orElseThrow(() -> new CustomException(LettersErrorCode.LETTER_NOT_FOUND));

        return new PlazaLetterReportAdminResDto(
                report.getReportId(),
                report.getLetter().getLetterId(),
                report.getLetter().getContent(),
                report.getReporterId(),
                report.getReason(),
                report.getDescription(),
                report.getStatus(),
                report.getCreatedAt()
        );
    }

    public PlazaLetterReplyReportAdminResDto getReportedReplyDetail(Long reportId) {
        PlazaLetterReplyReport report = replyReportRepository.findByIdWithReply(reportId)
                .orElseThrow(() -> new CustomException(LettersErrorCode.LETTER_NOT_FOUND));

        return new PlazaLetterReplyReportAdminResDto(
                report.getReportId(),
                report.getReply().getReplyId(),
                report.getReply().getText(),
                report.getReporterId(),
                report.getReason(),
                report.getDescription(),
                report.getStatus(),
                report.getCreatedAt()
        );
    }
}
