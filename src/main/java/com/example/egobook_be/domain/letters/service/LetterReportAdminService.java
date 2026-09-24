package com.example.egobook_be.domain.letters.service;

import com.example.egobook_be.domain.letters.dto.response.PlazaLetterReportAdminResDto;
import com.example.egobook_be.domain.moderation.entity.ModerationReportArchive;
import com.example.egobook_be.domain.moderation.entity.ReportStrike;
import com.example.egobook_be.domain.moderation.repository.ModerationReportArchiveRepository;
import com.example.egobook_be.domain.moderation.service.AdminReportMerge;
import com.example.egobook_be.global.enums.ReportReason;
import org.springframework.data.domain.SliceImpl;
import java.util.Comparator;
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
import com.example.egobook_be.domain.report.enums.ReportErrorCode;
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
    private final ModerationReportArchiveRepository archiveRepository;

    // 신고당한 유저(letter senderId / reply replierId)의 accountCode 조회, 탈퇴 등으로 id가 null이면 null 반환
    private String findAccountCode(Long userId) {
        if (userId == null) return null;
        return userRepository.findById(userId).map(User::getAccountCode).orElse(null);
    }

    private static ReportReason archiveReason(ModerationReportArchive a) {
        return a.getReason() == null ? null : ReportReason.valueOf(a.getReason());
    }

    private static ReportStatus archiveStatus(ModerationReportArchive a) {
        return a.getReportStatus() == null ? null : ReportStatus.valueOf(a.getReportStatus());
    }

    // 관리자가 이미 삭제된 콘텐츠임을 구분할 수 있도록 archived=true를 반환한다.
    public SliceResponse<PlazaLetterReportAdminResDto> getReportedLetters(int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 50);
        int prefix = AdminReportMerge.fetchSize(safePage, safeSize);
        Pageable prefixPage = PageRequest.of(0, prefix);
        List<PlazaLetterReportAdminResDto> live = letterReportRepository.findAllWithLetter(prefixPage)
                .stream().map(r -> new PlazaLetterReportAdminResDto(
                        r.getReportId(), r.getLetter().getLetterId(), r.getLetter().getContent(),
                        r.getReporterId(), r.getReason(), r.getDescription(), r.getStatus(), r.getAdminMemo(),
                        letterReportRepository.countByLetter_LetterId(r.getLetter().getLetterId()),
                        r.getCreatedAt(), r.getSenderId(), findAccountCode(r.getSenderId()), false)).toList();
        List<PlazaLetterReportAdminResDto> archived = archiveRepository
                .findBySourceTypeOrderByOriginalCreatedAtDescIdDesc(ReportStrike.SourceType.LETTER, prefixPage)
                .stream().map(a -> new PlazaLetterReportAdminResDto(
                        a.getSourceReportId(), a.getTargetContentId(), a.getOriginalContent(),
                        a.getReporterId(), archiveReason(a), a.getDescription(), archiveStatus(a), a.getAdminMemo(),
                        archiveRepository.countBySourceTypeAndTargetContentId(ReportStrike.SourceType.LETTER, a.getTargetContentId()),
                        a.getOriginalCreatedAt(), a.getTargetUserId(), findAccountCode(a.getTargetUserId()), true)).toList();
        Slice<PlazaLetterReportAdminResDto> merged = AdminReportMerge.merge(live, archived,
                Comparator.comparing(PlazaLetterReportAdminResDto::createdAt,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(PlazaLetterReportAdminResDto::reportId, Comparator.reverseOrder()),
                safePage, safeSize);
        return SliceResponse.of(merged, dto -> dto);
    }

    public SliceResponse<PlazaLetterReplyReportAdminResDto> getReportedReplies(int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 50);
        int prefix = AdminReportMerge.fetchSize(safePage, safeSize);
        Pageable prefixPage = PageRequest.of(0, prefix);
        List<PlazaLetterReplyReportAdminResDto> live = replyReportRepository.findAllWithReply(prefixPage)
                .stream().map(r -> new PlazaLetterReplyReportAdminResDto(
                        r.getReportId(), r.getReply().getReplyId(), r.getReply().getContent(),
                        r.getReporterId(), r.getReason(), r.getDescription(), r.getStatus(), r.getAdminMemo(),
                        replyReportRepository.countByReply_ReplyId(r.getReply().getReplyId()),
                        r.getCreatedAt(), r.getReplierId(), findAccountCode(r.getReplierId()), false)).toList();
        List<PlazaLetterReplyReportAdminResDto> archived = archiveRepository
                .findBySourceTypeOrderByOriginalCreatedAtDescIdDesc(ReportStrike.SourceType.REPLY, prefixPage)
                .stream().map(a -> new PlazaLetterReplyReportAdminResDto(
                        a.getSourceReportId(), a.getTargetContentId(), a.getOriginalContent(),
                        a.getReporterId(), archiveReason(a), a.getDescription(), archiveStatus(a), a.getAdminMemo(),
                        archiveRepository.countBySourceTypeAndTargetContentId(ReportStrike.SourceType.REPLY, a.getTargetContentId()),
                        a.getOriginalCreatedAt(), a.getTargetUserId(), findAccountCode(a.getTargetUserId()), true)).toList();
        Slice<PlazaLetterReplyReportAdminResDto> merged = AdminReportMerge.merge(live, archived,
                Comparator.comparing(PlazaLetterReplyReportAdminResDto::createdAt,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(PlazaLetterReplyReportAdminResDto::reportId, Comparator.reverseOrder()),
                safePage, safeSize);
        return SliceResponse.of(merged, dto -> dto);
    }

    // 상세 조회 기준을 reportId(개별 신고건)에서 letterId(신고된 컨텐츠)로 변경
    // - 같은 편지를 여러 명이 신고한 경우, 그 편지에 달린 모든 신고 내역을 리스트로 함께 반환
    public PlazaLetterReportDetailResDto getReportedLetterDetail(Long letterId) {
        List<PlazaLetterReport> reports = letterReportRepository.findAllByLetterId(letterId);
        List<ModerationReportArchive> archived = archiveRepository
                .findBySourceTypeAndTargetContentIdOrderByOriginalCreatedAtDescIdDesc(
                        ReportStrike.SourceType.LETTER, letterId);
        if (reports.isEmpty() && archived.isEmpty()) {
            throw new CustomException(LettersErrorCode.LETTER_NOT_FOUND);
        }
        List<ReportEntryResDto> entries = new java.util.ArrayList<>();
        for (PlazaLetterReport r : reports) {
            entries.add(ReportEntryResDto.builder().reportId(r.getReportId())
                    .reporterId(r.getReporterId()).reason(r.getReason()).description(r.getDescription())
                    .status(r.getStatus()).createdAt(r.getCreatedAt()).adminMemo(r.getAdminMemo()).build());
        }
        for (ModerationReportArchive a : archived) {
            entries.add(ReportEntryResDto.builder().reportId(a.getSourceReportId())
                    .reporterId(a.getReporterId()).reason(archiveReason(a)).description(a.getDescription())
                    .status(archiveStatus(a)).createdAt(a.getOriginalCreatedAt()).adminMemo(a.getAdminMemo()).build());
        }
        Long authorId = reports.isEmpty() ? archived.get(0).getTargetUserId() : reports.get(0).getSenderId();
        String content = reports.isEmpty() ? archived.get(0).getOriginalContent() : reports.get(0).getLetter().getContent();
        return new PlazaLetterReportDetailResDto(letterId, content, authorId,
                findAccountCode(authorId), entries.size(), entries);
    }

    public PlazaLetterReplyReportDetailResDto getReportedReplyDetail(Long replyId) {
        List<PlazaLetterReplyReport> reports = replyReportRepository.findAllByReplyId(replyId);
        List<ModerationReportArchive> archived = archiveRepository
                .findBySourceTypeAndTargetContentIdOrderByOriginalCreatedAtDescIdDesc(
                        ReportStrike.SourceType.REPLY, replyId);
        if (reports.isEmpty() && archived.isEmpty()) {
            throw new CustomException(LettersErrorCode.LETTER_NOT_FOUND);
        }
        List<ReportEntryResDto> entries = new java.util.ArrayList<>();
        for (PlazaLetterReplyReport r : reports) {
            entries.add(ReportEntryResDto.builder().reportId(r.getReportId())
                    .reporterId(r.getReporterId()).reason(r.getReason()).description(r.getDescription())
                    .status(r.getStatus()).createdAt(r.getCreatedAt()).adminMemo(r.getAdminMemo()).build());
        }
        for (ModerationReportArchive a : archived) {
            entries.add(ReportEntryResDto.builder().reportId(a.getSourceReportId())
                    .reporterId(a.getReporterId()).reason(archiveReason(a)).description(a.getDescription())
                    .status(archiveStatus(a)).createdAt(a.getOriginalCreatedAt()).adminMemo(a.getAdminMemo()).build());
        }
        Long authorId = reports.isEmpty() ? archived.get(0).getTargetUserId() : reports.get(0).getReplierId();
        String content = reports.isEmpty() ? archived.get(0).getOriginalContent() : reports.get(0).getReply().getContent();
        return new PlazaLetterReplyReportDetailResDto(replyId, content, authorId,
                findAccountCode(authorId), entries.size(), entries);
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
        PlazaLetterReport letterReport = letterReportRepository.findById(reportId).orElseThrow(() -> new CustomException(ReportErrorCode.REPORT_NOT_FOUND));
        letterReport.updateAdminMemo(memo);
        log.debug("[LetterReportAdminService] updateLetterReportMemo END - reportId: {}, memo: {}", reportId, memo);
    }

    @Transactional
    public void updateLetterReplyReportMemo(Long reportId, String memo) {
        log.debug("[LetterReportReplyAdminService] updateLetterReplyReportMemo START - reportId: {}, memo: {}", reportId, memo);
        PlazaLetterReplyReport letterReplyReport = replyReportRepository.findById(reportId).orElseThrow(() -> new CustomException(ReportErrorCode.REPORT_NOT_FOUND));
        letterReplyReport.updateAdminMemo(memo);
        log.debug("[LetterReportReplyAdminService] updateLetterReplyReportMemo END - reportId: {}, memo: {}", reportId, memo);
    }
}
