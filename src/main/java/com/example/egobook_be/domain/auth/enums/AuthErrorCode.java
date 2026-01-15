package com.example.egobook_be.domain.auth.enums;

import com.example.egobook_be.global.exception.model.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 사용자 인증 과정(Auth)에서 사용될 에러 코드들을 선언해둔 Enum Class
 */
@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements BaseErrorCode {
    /**
     * 400 BAD_REQUEST: 잘못된 요청
     */
    INVALID_TYPE_TOKEN(HttpStatus.BAD_REQUEST, "유효하지 않은 토큰 형식입니다."),
    INVALID_PROVIDER(HttpStatus.BAD_REQUEST, "유효하지 않은 Provider명입니다."),

    /**
     * 401 UNAUTHORIZED: 인증되지 않음
     */
    UID_NOT_FOUND(HttpStatus.UNAUTHORIZED, "해당 기기 식별자(UID)를 찾을 수 없습니다."),
    ACCESS_WITH_NON_ACCESS_TYPE_TOKEN(HttpStatus.UNAUTHORIZED, ""),

    /**
     * 500 INTERNAL_SERVER_ERROR: 내부 서버 오류
     */
    AUTH_ACCOUNT_USER_MISSING(HttpStatus.INTERNAL_SERVER_ERROR, "계정에 연결된 사용자 데이터가 누락되었습니다.");

    private final HttpStatus status;
    private final String message;
}
