package com.example.egobook_be.domain.user.enums;

import com.example.egobook_be.global.exception.model.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements BaseErrorCode {
    /*
     * 404 NOT FOUND
     */
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 사용자를 찾지 못했습니다."),
    ABILITY_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 사용자의 능력치 정보를 찾지 못했습니다.");


    private final HttpStatus status;
    private final String message;
}
