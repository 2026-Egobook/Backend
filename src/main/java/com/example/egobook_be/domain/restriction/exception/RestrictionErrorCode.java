package com.example.egobook_be.domain.restriction.exception;

import com.example.egobook_be.global.exception.model.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum RestrictionErrorCode implements BaseErrorCode {

    // 404
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "제재 대상 사용자를 찾을 수 없습니다."),
    RESTRICTION_NOT_FOUND(HttpStatus.NOT_FOUND,  "해당 제재 정보를 찾을 수 없습니다."),

    // 403
    LETTER_RESTRICTED(HttpStatus.FORBIDDEN, "편지 기능이 제한된 사용자입니다."),
    QUESTION_ANSWER_RESTRICTED(HttpStatus.FORBIDDEN, "오늘의 질문 답변 기능이 제한된 사용자입니다."),

    // 409
    ALREADY_RESTRICTED(HttpStatus.CONFLICT, "이미 해당 도메인에 대한 제재가 적용 중입니다."),
    ALREADY_CANCELED(HttpStatus.CONFLICT,  "이미 관리자에 의해 해제된 제재입니다."),
    ALREADY_EXPIRED(HttpStatus.CONFLICT, "이미 유효 기간이 지나서 해제된 제재입니다.");

    private final HttpStatus status;
    private final String message;
}
