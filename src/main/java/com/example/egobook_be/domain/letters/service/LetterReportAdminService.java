package com.example.egobook_be.domain.letters.service;

import com.example.egobook_be.domain.letters.dto.response.PlazaLetterReportAdminResDto;
import com.example.egobook_be.domain.letters.dto.response.PlazaLetterReportDetailResDto;
import com.example.egobook_be.domain.letters.dto.response.PlazaLetterReplyReportAdminResDto;
import com.example.egobook_be.domain.letters.dto.response.PlazaLetterReplyReportDetailResDto;
import com.example.egobook_be.domain.letters.entity.PlazaLetterReport;
import com.example.egobook_be.domain.letters.entity.PlazaLetterReplyReport;
import com.example.egobook_be.domain.letters.enums.LettersErrorCode;
import com.example.egobook_be.domain.letters.repository.PlazaLetterReplyRepository;
import com.example.egobook_be.domain.letters.repository.PlazaLetterReportRepository;
import com.example.egobook_be.domain.letters.repository.PlazaLetterReplyReportRepository;
import com.example.egobook_be.domain.letters.repository.PlazaLetterRepository;
import com.example.egobook_be.domain.report.dto.ReportEntryResDto;
import com.example.egobook_be.domain.user.entity.User;
import com.example.egobook_be.domain.user.repository.UserRepository;
import com.example.egobook_be.global.enums.ReportStatus;
import com.example.egobook_be.global.exception.CustomException;
import com.example.egobook_be.global.response.SliceResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LetterReportAdminService {

    private final PlazaLetterReportRepository letterReportRepository;
    private final PlazaLetterReplyReportRepository replyReportRepository;
    private final PlazaLetterRepository letterRepository;
    private final PlazaLetterReplyRepository replyRepository;
    private final UserRepository userRepository;

    // 신고당한 유저(letter senderId / reply replierId)의 accountCode 조회, 탈퇴 등으로 id가 null이면 null 반환
    private String findAccountCode(Long userId) {
        if (userId == null) return null;
        return userRepository.findById(userId).map(User::getAccountCode).orElse(null);
    }

    public SliceResponse<PlazaLetterReportAdminResDto> getReportedLetters(int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 50);
        Pageable pageable = PageRequest.of(safePage - 1, safeSize);

        Slice<PlazaLetterReport> slice = letterReportRepository.findAllWithLetter(pageable);

        return SliceResponse.of(slice, report -> {
            long reportCount = letterReportRepository.countByLetter_LetterId(report.getLetter().getLetterId());

            return new PlazaLetterReportAdminResDto(
                report.getReportId(),
                report.getLetter().getLetterId(),
                report.getLetter().getContent(),
                report.getReporterId(),
                report.getReason(),
                report.getDescription(),
                report.getStatus(),
                report.getAdminMemo(),
                reportCount,
                report.getCreatedAt(),
                report.getSenderId(),
                findAccountCode(report.getSenderId())
            );
        });
    }

    public SliceResponse<PlazaLetterReplyReportAdminResDto> getReportedReplies(int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 50);
        Pageable pageable = PageRequest.of(safePage - 1, safeSize);

        Slice<PlazaLetterReplyReport> slice = replyReportRepository.findAllWithReply(pageable);

        return SliceResponse.of(slice, report -> {
            long reportCount = replyReportRepository.countByReply_ReplyId(report.getReply().getReplyId());

            return new PlazaLetterReplyReportAdminResDto(
                report.getReportId(),
                report.getReply().getReplyId(),
                report.getReply().getContent(),
                report.getReporterId(),
                report.getReason(),
                report.getDescription(),
                report.getStatus(),
                report.getAdminMemo(),
                reportCount,
                report.getCreatedAt(),
                report.getReplierId(),
                findAccountCode(report.getReplierId())
            );
        });
    }

    // 상세 조회 기준을 reportId(개별 신고건)에서 letterId(신고된 컨텐츠)로 변경
    // - 같은 편지를 여러 명이 신고한 경우, 그 편지에 달린 모든 신고 내역을 리스트로 함께 반환
    public PlazaLetterReportDetailResDto getReportedLetterDetail(Long letterId) {
        List<PlazaLetterReport> reports = letterReportRepository.findAllByLetterId(letterId);
        if (reports.isEmpty()) {
            throw new CustomException(LettersErrorCode.LETTER_NOT_FOUND);
        }
        PlazaLetterReport first = reports.get(0);

        List<ReportEntryResDto> entries = reports.stream()
                .map(r -> ReportEntryResDto.builder()
                        .reportId(r.getReportId())
                        .reporterId(r.getReporterId())
                        .reason(r.getReason())
                        .description(r.getDescription())
                        .status(r.getStatus())
                        .createdAt(r.getCreatedAt())
                        .build())
                .toList();

        return new PlazaLetterReportDetailResDto(
                letterId,
                first.getLetter().getContent(),
                first.getSenderId(),
                findAccountCode(first.getSenderId()),
                entries.size(),
                entries
        );
    }

    // 상세 조회 기준을 reportId(개별 신고건)에서 replyId(신고된 컨텐츠)로 변경
    public PlazaLetterReplyReportDetailResDto getReportedReplyDetail(Long replyId) {
        List<PlazaLetterReplyReport> reports = replyReportRepository.findAllByReplyId(replyId);
        if (reports.isEmpty()) {
            throw new CustomException(LettersErrorCode.LETTER_NOT_FOUND);
        }
        PlazaLetterReplyReport first = reports.get(0);

        List<ReportEntryResDto> entries = reports.stream()
                .map(r -> ReportEntryResDto.builder()
                        .reportId(r.getReportId())
                        .reporterId(r.getReporterId())
                        .reason(r.getReason())
                        .description(r.getDescription())
                        .status(r.getStatus())
                        .createdAt(r.getCreatedAt())
                        .build())
                .toList();

        return new PlazaLetterReplyReportDetailResDto(
                replyId,
                first.getReply().getContent(),
                first.getReplierId(),
                findAccountCode(first.getReplierId()),
                entries.size(),
                entries
        );
    }

    //수동 삭제
    @Transactional
    public void deleteLetter(Long letterId) {
        if (!letterRepository.existsById(letterId)) {
            throw new CustomException(LettersErrorCode.LETTER_NOT_FOUND);
        }
        letterReportRepository.deleteAllByLetterId(letterId);   // 신고 내역 먼저 삭제
        letterRepository.deleteById(letterId);
    }

    @Transactional
    public void deleteReply(Long replyId) {
        if (!replyRepository.existsById(replyId)) {
            throw new CustomException(LettersErrorCode.LETTER_NOT_FOUND);
        }
        replyReportRepository.deleteAllByReplyId(replyId);      // 신고 내역 먼저 삭제
        replyRepository.deleteById(replyId);
    }

    @Transactional
    public void approveLetterReport(Long reportId) {
        PlazaLetterReport report = letterReportRepository.findByIdWithLetter(reportId)
                .orElseThrow(() -> new CustomException(LettersErrorCode.LETTER_NOT_FOUND));

        if (report.getStatus() != ReportStatus.PENDING) {
            throw new CustomException(LettersErrorCode.REPORT_ALREADY_RESOLVED);
        }

        report.approve();

        long approvedCount = letterReportRepository
                .countByLetterIdAndStatus(report.getLetter().getLetterId(), ReportStatus.RESOLVED);

        if (approvedCount >= 3) {
            report.getLetter().hide();
        }
    }

    @Transactional
    // 반려 시 상태 변경 대신 신고 이력 자체를 삭제
    public void rejectLetterReport(Long reportId) {
        PlazaLetterReport report = letterReportRepository.findByIdWithLetter(reportId)
                .orElseThrow(() -> new CustomException(LettersErrorCode.LETTER_NOT_FOUND));

        if (report.getStatus() != ReportStatus.PENDING) {
            throw new CustomException(LettersErrorCode.REPORT_ALREADY_RESOLVED);
        }

        letterReportRepository.delete(report);
    }

    @Transactional
    public void approveReplyReport(Long reportId) {
        PlazaLetterReplyReport report = replyReportRepository.findByIdWithReply(reportId)
                .orElseThrow(() -> new CustomException(LettersErrorCode.LETTER_NOT_FOUND));

        if (report.getStatus() != ReportStatus.PENDING) {
            throw new CustomException(LettersErrorCode.REPORT_ALREADY_RESOLVED);
        }

        report.approve();

        long approvedCount = replyReportRepository
                .countByReplyIdAndStatus(report.getReply().getReplyId(), ReportStatus.RESOLVED);

        if (approvedCount >= 3) {
            report.getReply().hide();
        }
    }

    @Transactional
    // 반려 시 상태 변경 대신 신고 이력 자체를 삭제
    public void rejectReplyReport(Long reportId) {
        PlazaLetterReplyReport report = replyReportRepository.findByIdWithReply(reportId)
                .orElseThrow(() -> new CustomException(LettersErrorCode.LETTER_NOT_FOUND));

        if (report.getStatus() != ReportStatus.PENDING) {
            throw new CustomException(LettersErrorCode.REPORT_ALREADY_RESOLVED);
        }

        replyReportRepository.delete(report);
    }

    @Transactional
    public void updateLetterReportMemo(Long reportId, String memo) {
        log.debug("[LetterReportAdminService] updateLetterReportMemo START - reportId: {}, memo: {}", reportId, memo);
        PlazaLetterReport letterReport = letterReportRepository.findById(reportId).orElseThrow(() -> new CustomException(LettersErrorCode.LETTER_NOT_FOUND));
        letterReport.updateAdminMemo(memo);
        log.debug("[LetterReportAdminService] updateLetterReportMemo END - reportId: {}, memo: {}", reportId, memo);
    }

    @Transactional
    public void updateLetterReplyReportMemo(Long reportId, String memo) {
        log.debug("[LetterReportReplyAdminService] updateLetterReplyReportMemo START - reportId: {}, memo: {}", reportId, memo);
        PlazaLetterReplyReport letterReplyReport = replyReportRepository.findById(reportId).orElseThrow(() -> new CustomException(LettersErrorCode.LETTER_NOT_FOUND));
        letterReplyReport.updateAdminMemo(memo);
        log.debug("[LetterReportReplyAdminService] updateLetterReplyReportMemo END - reportId: {}, memo: {}", reportId, memo);
    }
}
