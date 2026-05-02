package com.example.egobook_be.domain.question.exception;

import com.example.egobook_be.global.exception.model.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum QuestionErrorCode implements BaseErrorCode {

    /**
     * 404 NOT_FOUND
     */
    TODAY_QUESTION_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "오늘의 질문이 존재하지 않습니다."
    ),

    ANSWER_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "해당 질문에 대한 답변을 찾을 수 없습니다."
    ),

    /**
     * 409 CONFLICT
     */
    ALREADY_ANSWERED_TODAY(
            HttpStatus.CONFLICT,
            "오늘의 질문에는 이미 답변을 작성했습니다."
    ),

    ALREADY_REPORTED(HttpStatus.CONFLICT, "이미 신고한 질문입니다."),

    /**
     * 400 BAD_REQUEST
     */
    INVALID_VISIBILITY(
            HttpStatus.BAD_REQUEST,
            "유효하지 않은 공개 범위입니다."
    ),

    INVALID_QUESTION_ID(
            HttpStatus.CONFLICT,
            "해당 id가 존재하지 않습니다."
    ),

    DUPLICATE_QUESTION_DATE(
            HttpStatus.CONFLICT,
            "해당 날짜에는 이미 활성화된 질문이 존재합니다."
    );

    private final HttpStatus status;
    private final String message;
}
