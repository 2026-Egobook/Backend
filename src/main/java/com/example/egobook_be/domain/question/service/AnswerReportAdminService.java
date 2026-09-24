package com.example.egobook_be.domain.question.service;

import com.example.egobook_be.domain.moderation.entity.ModerationReportArchive;
import com.example.egobook_be.domain.moderation.entity.ReportStrike;
import com.example.egobook_be.domain.moderation.repository.ModerationReportArchiveRepository;
import com.example.egobook_be.domain.moderation.service.AdminReportMerge;
import com.example.egobook_be.global.enums.ReportReason;
import org.springframework.data.domain.Slice;
import java.util.Comparator;
import com.example.egobook_be.domain.letters.enums.LettersErrorCode;
import com.example.egobook_be.domain.question.dto.AnswerReportAdminResDto;
import com.example.egobook_be.domain.question.dto.AnswerReportDetailResDto;
import com.example.egobook_be.domain.question.entity.AnswerReport;
import com.example.egobook_be.domain.question.enums.AnswerVisibility;
import com.example.egobook_be.domain.question.exception.QuestionErrorCode;
import com.example.egobook_be.domain.question.repository.AnswerReportRepository;
import com.example.egobook_be.domain.question.repository.QuestionAnswerRepository;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnswerReportAdminService {

    private final AnswerReportRepository answerReportRepository;
    private final QuestionAnswerRepository questionAnswerRepository;
    private final UserRepository userRepository;
    private final ModerationReportArchiveRepository archiveRepository;

    // 신고당한 유저(답변 작성자)의 accountCode 조회, 탈퇴 등으로 id가 null이면 null 반환
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

    @Transactional(readOnly = true)
    public SliceResponse<AnswerReportAdminResDto> getReportedAnswers(int page, int size) {
        int safePage = Math.max(1, page);
        int safeSize = Math.min(Math.max(1, size), 50);
        Pageable prefixPage = PageRequest.of(0, AdminReportMerge.fetchSize(safePage, safeSize));
        List<AnswerReportAdminResDto> live = answerReportRepository.findAllWithAnswerAndUser(prefixPage)
                .stream().map(this::toDto).toList();
        List<AnswerReportAdminResDto> archived = archiveRepository
                .findBySourceTypeOrderByOriginalCreatedAtDescIdDesc(ReportStrike.SourceType.ANSWER, prefixPage)
                .stream().map(a -> new AnswerReportAdminResDto(
                        a.getSourceReportId(), a.getTargetContentId(), a.getOriginalContent(),
                        a.getReporterId(), a.getReporterId() == null ? null : userRepository.findById(a.getReporterId())
                        .map(User::getNickname).orElse(null), archiveReason(a), a.getDescription(),
                        archiveRepository.countBySourceTypeAndTargetContentId(ReportStrike.SourceType.ANSWER, a.getTargetContentId()),
                        archiveStatus(a), a.getAdminMemo(), a.getOriginalCreatedAt(), a.getTargetUserId(),
                        findAccountCode(a.getTargetUserId()), true)).toList();
        Slice<AnswerReportAdminResDto> merged = AdminReportMerge.merge(live, archived,
                Comparator.comparing(AnswerReportAdminResDto::reportedAt,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(AnswerReportAdminResDto::reportId, Comparator.reverseOrder()),
                safePage, safeSize);
        return SliceResponse.of(merged, dto -> dto);
    }

    private AnswerReportAdminResDto toDto(AnswerReport report) {
        long reportCount = answerReportRepository.countByAnswerId(report.getAnswer().getId());
        return new AnswerReportAdminResDto(
                report.getId(), report.getAnswer().getId(), report.getAnswer().getContent(),
                report.getUser().getId(), report.getUser().getNickname(), report.getReason(),
                report.getDescription(), reportCount, report.getStatus(), report.getAdminMemo(),
                report.getCreatedAt(), report.getAnswer().getUser().getId(),
                findAccountCode(report.getAnswer().getUser().getId()), false);
    }

    @Transactional(readOnly = true)
    public AnswerReportDetailResDto getReportedAnswerDetail(Long answerId) {
        List<AnswerReport> reports = answerReportRepository.findAllByAnswerId(answerId);
        List<ModerationReportArchive> archived = archiveRepository
                .findBySourceTypeAndTargetContentIdOrderByOriginalCreatedAtDescIdDesc(
                        ReportStrike.SourceType.ANSWER, answerId);
        if (reports.isEmpty() && archived.isEmpty()) {
            throw new CustomException(QuestionErrorCode.ANSWER_NOT_FOUND);
        }
        List<ReportEntryResDto> entries = new java.util.ArrayList<>();
        for (AnswerReport r : reports) {
            entries.add(ReportEntryResDto.builder().reportId(r.getId())
                    .reporterId(r.getUser().getId()).reason(r.getReason()).description(r.getDescription())
                    .status(r.getStatus()).createdAt(r.getCreatedAt()).adminMemo(r.getAdminMemo()).build());
        }
        for (ModerationReportArchive a : archived) {
            entries.add(ReportEntryResDto.builder().reportId(a.getSourceReportId())
                    .reporterId(a.getReporterId()).reason(archiveReason(a)).description(a.getDescription())
                    .status(archiveStatus(a)).createdAt(a.getOriginalCreatedAt()).adminMemo(a.getAdminMemo()).build());
        }
        Long authorId = reports.isEmpty() ? archived.get(0).getTargetUserId() : reports.get(0).getAnswer().getUser().getId();
        String content = reports.isEmpty() ? archived.get(0).getOriginalContent() : reports.get(0).getAnswer().getContent();
        return new AnswerReportDetailResDto(answerId, content, authorId,
                findAccountCode(authorId), entries.size(), entries);
    }

    //수동 삭제
    @Transactional
    public void deleteAnswer(Long answerId) {
        log.info("[AnswerReportAdminService] deleteAnswer Start - answerId: {}", answerId);
        if (!questionAnswerRepository.existsById(answerId)) {
            throw new CustomException(QuestionErrorCode.ANSWER_NOT_FOUND);
        }
        answerReportRepository.deleteAllByAnswerId(answerId);   // 신고 내역 먼저 삭제
        questionAnswerRepository.deleteById(answerId);
        log.info("[AnswerReportAdminService] deleteAnswer End - answerId: {}", answerId);
    }

    @Transactional
    public void approveAnswerReport(Long reportId) {
        AnswerReport report = answerReportRepository.findByIdWithAnswerAndUser(reportId)
                .orElseThrow(() -> new CustomException(QuestionErrorCode.ANSWER_NOT_FOUND));

        if (report.getStatus() != ReportStatus.PENDING) {
            throw new CustomException(QuestionErrorCode.ALREADY_RESOLVED);
        }

        report.approve();

        long approvedCount = answerReportRepository
                .countByAnswerIdAndStatus(report.getAnswer().getId(), ReportStatus.RESOLVED);

        if (approvedCount >= 3) {
            report.getAnswer().update(report.getAnswer().getContent(), AnswerVisibility.PRIVATE);
        }
    }


    @Transactional
    public void rejectAnswerReport(Long reportId) {
        AnswerReport report = answerReportRepository.findByIdWithAnswerAndUser(reportId)
                .orElseThrow(() -> new CustomException(QuestionErrorCode.ANSWER_NOT_FOUND));

        if (report.getStatus() != ReportStatus.PENDING) {
            throw new CustomException(QuestionErrorCode.ALREADY_RESOLVED);
        }

        answerReportRepository.delete(report);
    }

    @Transactional
    public void updateAnswerReportMemo(Long reportId, String memo) {
        log.debug("[AnswerReportAdminService] updateAnswerReportMemo START - reportId: {}, memo: {}", reportId, memo);
        AnswerReport answerReport = answerReportRepository.findById(reportId).orElseThrow(() -> new CustomException(ReportErrorCode.REPORT_NOT_FOUND));
        answerReport.updateAdminMemo(memo);
        log.debug("[AnswerReportAdminService] updateAnswerReportMemo END - reportId: {}, memo: {}", reportId, memo);
    }
}
