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
    UNFULFILLED_REQUEST_VALUES(HttpStatus.BAD_REQUEST, "요청 값이 전부 충족되지 않았습니다."),

    /**
     * 401 UNAUTHORIZED: 인증되지 않음
     */
    ACCESS_WITH_NON_ACCESS_TYPE_TOKEN(HttpStatus.UNAUTHORIZED, "Access Token이 아닌 토큰으로 접근을 시도하였습니다."),
    ACCESS_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "Access Token이 만료되었습니다. Refresh Token을 사용하여 Access Token을 재발급받으세요."),
    REFRESH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "Refresh Token이 만료되었습니다. Recover Token을 사용하여 Refresh Token을 재발급받으세요."),

    /**
     * 404 NOT_FOUND: 리소스 없음
     */
    UID_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 기기 식별자(UID)를 찾을 수 없습니다."),
    AUTH_ACCOUNT_NOT_FOUND_IN_REFRESH_TOKEN_BACKUP(HttpStatus.NOT_FOUND , "Refresh Token Backup Table에서 해당 Auth Account 인스턴스를 찾을 수 없습니다."),
    REFRESH_TOKEN_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 Refresh Token에 대한 정보를 서버에서 찾을 수 없습니다. Recover Token을 사용하여 Refresh Token을 재발급받으세요."),

    /**
     * 409 CONFLICT : 충돌 (ex: 중복 데이터)
     */
    ALREADY_REGISTERED_DEVICE(HttpStatus.CONFLICT, "이미 서버에 등록된 기기입니다. /auth/guest/login이나 /auth/google/login을 시도해주세요."),

    /**
     * 500 INTERNAL_SERVER_ERROR: 내부 서버 오류
     */
    AUTH_ACCOUNT_USER_MISSING(HttpStatus.INTERNAL_SERVER_ERROR, "계정에 연결된 사용자 데이터가 누락되었습니다."),
    NICKNAME_GENERATE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "닉네임 생성 실패: 재시도 횟수를 초과하였습니다.");

    private final HttpStatus status;
    private final String message;
}
