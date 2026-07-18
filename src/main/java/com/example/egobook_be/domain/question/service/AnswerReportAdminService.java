package com.example.egobook_be.domain.question.service;

import com.example.egobook_be.domain.letters.entity.PlazaLetterReport;
import com.example.egobook_be.domain.letters.enums.LettersErrorCode;
import com.example.egobook_be.domain.question.dto.AnswerReportAdminResDto;
import com.example.egobook_be.domain.question.dto.AnswerReportDetailResDto;
import com.example.egobook_be.domain.question.entity.AnswerReport;
import com.example.egobook_be.domain.question.enums.AnswerVisibility;
import com.example.egobook_be.domain.question.exception.QuestionErrorCode;
import com.example.egobook_be.domain.question.repository.AnswerReportRepository;
import com.example.egobook_be.domain.question.repository.QuestionAnswerRepository;
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

    // 신고당한 유저(답변 작성자)의 accountCode 조회, 탈퇴 등으로 id가 null이면 null 반환
    private String findAccountCode(Long userId) {
        if (userId == null) return null;
        return userRepository.findById(userId).map(User::getAccountCode).orElse(null);
    }

    @Transactional(readOnly = true)
    public SliceResponse<AnswerReportAdminResDto> getReportedAnswers(
            int page,
            int size
    ) {
        log.info("[AnswerReportAdminService] getReportedAnswers Start");
        Pageable pageable = PageRequest.of(page-1, size);

        log.info("[AnswerReportAdminService] getReportedAnswers End");
        return SliceResponse.of(
                answerReportRepository.findAllWithAnswerAndUser(pageable),
                this::toDto
        );
    }

    private AnswerReportAdminResDto toDto(AnswerReport report) {
        long reportCount = answerReportRepository.countByAnswerId(report.getAnswer().getId());

        return new AnswerReportAdminResDto(
                report.getId(),
                report.getAnswer().getId(),
                report.getAnswer().getContent(),
                report.getUser().getId(),
                report.getUser().getNickname(),
                report.getReason(),
                report.getDescription(),
                reportCount,
                report.getStatus(),
                report.getAdminMemo(),
                report.getCreatedAt(),
                report.getAnswer().getUser().getId(),
                findAccountCode(report.getAnswer().getUser().getId())
        );
    }


    @Transactional(readOnly = true)
    public AnswerReportDetailResDto getReportedAnswerDetail(Long answerId) {
        log.info("[AnswerReportAdminService] getReportedAnswerDetail Start - answerId: {}", answerId);
        List<AnswerReport> reports = answerReportRepository.findAllByAnswerId(answerId);
        if (reports.isEmpty()) {
            throw new CustomException(QuestionErrorCode.ANSWER_NOT_FOUND);
        }
        AnswerReport first = reports.get(0);

        List<ReportEntryResDto> entries = reports.stream()
                .map(r -> ReportEntryResDto.builder()
                        .reportId(r.getId())
                        .reporterId(r.getUser().getId())
                        .reason(r.getReason())
                        .description(r.getDescription())
                        .status(r.getStatus())
                        .createdAt(r.getCreatedAt())
                        .build())
                .toList();

        log.info("[AnswerReportAdminService] getReportedAnswerDetail End - answerId: {}", answerId);
        return new AnswerReportDetailResDto(
                answerId,
                first.getAnswer().getContent(),
                first.getAnswer().getUser().getId(),
                findAccountCode(first.getAnswer().getUser().getId()),
                entries.size(),
                entries
        );
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
        AnswerReport answerReport = answerReportRepository.findById(reportId).orElseThrow(() -> new CustomException(QuestionErrorCode.ANSWER_NOT_FOUND));
        answerReport.updateAdminMemo(memo);
        log.debug("[AnswerReportAdminService] updateAnswerReportMemo END - reportId: {}, memo: {}", reportId, memo);
    }
}