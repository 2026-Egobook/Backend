package com.example.egobook_be.domain.restriction.exception;

import com.example.egobook_be.global.exception.model.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum RestrictionErrorCode implements BaseErrorCode {

    // 404
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "RESTRICTION404_USER_NOT_FOUND", "제재 대상 사용자를 찾을 수 없습니다."),

    // 409
    ALREADY_RESTRICTED(HttpStatus.CONFLICT, "RESTRICTION409_ALREADY_RESTRICTED", "이미 해당 도메인에 대한 제재가 적용 중입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
