package com.example.egobook_be.domain.question.mapper;

import com.example.egobook_be.domain.question.dto.MyAnswerHistoryResDto;
import com.example.egobook_be.domain.question.entity.QuestionAnswer;
import com.example.egobook_be.domain.question.entity.TodayQuestion;

public class MyAnswerHistoryMapper {

    public static MyAnswerHistoryResDto toDto(QuestionAnswer answer) {
        TodayQuestion question = answer.getQuestion();

        return MyAnswerHistoryResDto.builder()
                .questionId(question.getId())
                .questionDate(question.getQuestionDate())
                .questionContent(question.getContent())
                .answerId(answer.getId())
                .answerContent(answer.getContent())
                .visibility(answer.getVisibility())
                .answeredAt(answer.getCreatedAt())
                .build();
    }
}
