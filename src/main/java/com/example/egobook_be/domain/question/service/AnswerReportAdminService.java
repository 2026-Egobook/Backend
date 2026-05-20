package com.example.egobook_be.domain.question.service;

import com.example.egobook_be.domain.question.dto.AnswerReportAdminResDto;
import com.example.egobook_be.domain.question.entity.AnswerReport;
import com.example.egobook_be.domain.question.enums.AnswerVisibility;
import com.example.egobook_be.domain.question.exception.QuestionErrorCode;
import com.example.egobook_be.domain.question.repository.AnswerReportRepository;
import com.example.egobook_be.domain.question.repository.QuestionAnswerRepository;
import com.example.egobook_be.global.enums.ReportStatus;
import com.example.egobook_be.global.exception.CustomException;
import com.example.egobook_be.global.response.SliceResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnswerReportAdminService {

    private final AnswerReportRepository answerReportRepository;
    private final QuestionAnswerRepository questionAnswerRepository;

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
                report.getCreatedAt()
        );
    }

    @Transactional(readOnly = true)
    public AnswerReportAdminResDto getReportedAnswerDetail(Long reportId) {
        log.info("[AnswerReportAdminService] getReportedAnswers Start - reportId: {}", reportId);
        AnswerReport report = answerReportRepository.findByIdWithAnswerAndUser(reportId)
                .orElseThrow(() -> new CustomException(QuestionErrorCode.ANSWER_NOT_FOUND));

        log.info("[AnswerReportAdminService] getReportedAnswers End - reportId: {}", reportId);
        return toDto(report);
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
    public void approveReport(Long reportId) {
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
    public void rejectReport(Long reportId) {
        AnswerReport report = answerReportRepository.findByIdWithAnswerAndUser(reportId)
                .orElseThrow(() -> new CustomException(QuestionErrorCode.ANSWER_NOT_FOUND));

        if (report.getStatus() != ReportStatus.PENDING) {
            throw new CustomException(QuestionErrorCode.ALREADY_RESOLVED);
        }

        report.reject();
    }
}