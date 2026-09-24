package com.example.egobook_be.domain.question.service;

import com.example.egobook_be.domain.moderation.entity.ReportStrike;
import com.example.egobook_be.domain.moderation.service.AutomaticRestrictionService;
import com.example.egobook_be.domain.question.dto.AnswerReportReqDto;
import com.example.egobook_be.domain.question.dto.AnswerReportResDto;
import com.example.egobook_be.domain.question.entity.AnswerReport;
import com.example.egobook_be.domain.question.entity.QuestionAnswer;
import com.example.egobook_be.domain.question.enums.AnswerVisibility;
import com.example.egobook_be.domain.question.exception.QuestionErrorCode;
import com.example.egobook_be.domain.question.repository.AnswerReportRepository;
import com.example.egobook_be.domain.question.repository.QuestionAnswerRepository;
import com.example.egobook_be.domain.user.entity.User;
import com.example.egobook_be.domain.user.repository.UserRepository;
import com.example.egobook_be.global.enums.ReportStatus;
import com.example.egobook_be.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AnswerReportService {

    private final AnswerReportRepository answerReportRepository;
    private final QuestionAnswerRepository questionAnswerRepository;
    private final UserRepository userRepository;
    private final AutomaticRestrictionService automaticRestrictionService;

    public AnswerReportResDto reportAnswer(
            Long userId,
            Long answerId,
            AnswerReportReqDto reqDto
    ) {
        User user = userRepository.findById(userId).orElseThrow();

        QuestionAnswer answer = questionAnswerRepository.findById(answerId)
                .orElseThrow(() -> new CustomException(QuestionErrorCode.ANSWER_NOT_FOUND));

        Long authorId = answer.getUser().getId();
        automaticRestrictionService.lockAuthor(authorId);
        if (answerReportRepository.existsByUserAndAnswer(user, answer)) {
            throw new CustomException(QuestionErrorCode.ALREADY_REPORTED);
        }


        AnswerReport report = answerReportRepository.saveAndFlush(
                AnswerReport.builder()
                        .user(user)
                        .answer(answer)
                        .reason(reqDto.reason())
                        .description(reqDto.description())
                        .status(ReportStatus.PENDING)
                        .build()
        );

        // Capture response BEFORE cleanup deletes the question answer/report at threshold.
        AnswerReportResDto result = new AnswerReportResDto(
                report.getId(), answerId, report.getReason(), report.getCreatedAt());
        automaticRestrictionService.onReportSaved(ReportStrike.SourceType.ANSWER,
                report.getId(), authorId, answerId);
        return result;
    }
}
