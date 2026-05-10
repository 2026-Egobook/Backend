package com.example.egobook_be.domain.question.mapper;

import com.example.egobook_be.domain.question.dto.PublicAnswerResDto;
import com.example.egobook_be.domain.question.entity.QuestionAnswer;
import com.example.egobook_be.domain.user.entity.Ability;

public class PublicAnswerMapper {

    public static PublicAnswerResDto toDto(QuestionAnswer answer, Ability ability) {
        return PublicAnswerResDto.builder()
                .answerId(answer.getId())
                .userId(answer.getUser().getId())
                .nickname(answer.getUser().getNickname())
                .level(answer.getUser().getLevel())
                .topAbilityName(ability.getTopAbilityName())
                .content(answer.getContent())
                .createdAt(answer.getCreatedAt())
                .build();
    }
}
