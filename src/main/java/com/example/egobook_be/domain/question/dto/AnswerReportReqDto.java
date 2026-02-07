package com.example.egobook_be.domain.question.dto;

import com.example.egobook_be.domain.question.enums.QuestionReportReason;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AnswerReportReqDto(

        @NotNull
        QuestionReportReason reason,

        @Size(max = 500)
        String description
) {}