package com.example.egobook_be.domain.question.dto;

import com.example.egobook_be.domain.question.enums.AnswerVisibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AnswerCreateReqDto(
        @NotBlank(message = "답변 내용은 필수입니다.")
        String content,

        @NotNull(message = "공개 범위는 필수입니다.")
        AnswerVisibility visibility
) {
}
