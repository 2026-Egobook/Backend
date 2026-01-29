package com.example.egobook_be.domain.psychology.exception;

import com.example.egobook_be.global.exception.model.BaseErrorCode; import lombok.Getter; import lombok.RequiredArgsConstructor; import org.springframework.http.HttpStatus;

@Getter @RequiredArgsConstructor public enum PsychologyErrorCode implements BaseErrorCode {

    KNOWLEDGE_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 심리지식을 찾을 수 없습니다."),
    USER_KNOWLEDGE_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 지식의 조회 기록을 찾을 수 없습니다."),

    ALREADY_BOOKMARKED(HttpStatus.CONFLICT, "이미 저장된 심리지식입니다."),
    NOT_BOOKMARKED(HttpStatus.BAD_REQUEST, "북마크되지 않은 지식은 취소할 수 없습니다."),
    USER_NOT_FOUND(HttpStatus.BAD_REQUEST, "유저를 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String message;
}