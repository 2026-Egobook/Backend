package com.example.egobook_be.domain.question.mapper;

import com.example.egobook_be.domain.question.dto.PublicAnswerResDto;
import com.example.egobook_be.domain.question.entity.QuestionAnswer;

public class PublicAnswerMapper {

    public static PublicAnswerResDto toDto(QuestionAnswer answer) {
        return PublicAnswerResDto.builder()
                .answerId(answer.getId())
                .userId(answer.getUser().getId())
                .nickname(answer.getUser().getNickname())
                .content(answer.getContent())
                .createdAt(answer.getCreatedAt())
                .build();
    }
}
