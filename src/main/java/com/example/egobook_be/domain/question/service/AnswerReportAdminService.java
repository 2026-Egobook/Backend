package com.example.egobook_be.domain.question.service;

import com.example.egobook_be.domain.question.dto.AnswerReportAdminResDto;
import com.example.egobook_be.domain.question.entity.AnswerReport;
import com.example.egobook_be.domain.question.repository.AnswerReportRepository;
import com.example.egobook_be.global.response.SliceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AnswerReportAdminService {

    private final AnswerReportRepository answerReportRepository;

    @Transactional(readOnly = true)
    public SliceResponse<AnswerReportAdminResDto> getReportedAnswers(
            int page,
            int size
    ) {
        Pageable pageable = PageRequest.of(page-1, size);

        return SliceResponse.of(
                answerReportRepository.findAllWithAnswerAndUser(pageable),
                this::toDto
        );
    }

    private AnswerReportAdminResDto toDto(AnswerReport report) {
        return new AnswerReportAdminResDto(
                report.getId(),
                report.getAnswer().getId(),
                report.getAnswer().getContent(),
                report.getUser().getId(),
                report.getUser().getNickname(),
                report.getReason(),
                report.getDescription(),
                report.getCreatedAt()
        );
    }
}