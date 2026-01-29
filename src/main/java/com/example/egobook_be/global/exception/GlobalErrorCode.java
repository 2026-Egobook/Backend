package com.example.egobook_be.global.exception;

import com.example.egobook_be.global.enums.GlobalResponseCode;
import com.example.egobook_be.global.exception.model.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 전역에서 사용될 에러 코드들을 선언해둔 Enum Class
 */
@Getter
@RequiredArgsConstructor
public enum GlobalErrorCode implements BaseErrorCode {
    /**
     * 400 BAD_REQUEST: 잘못된 요청
     */
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),
    INVALID_TYPE_VALUE(HttpStatus.BAD_REQUEST, "입력값의 타입이 유효하지 않습니다."),
    MISSING_INPUT_VALUE(HttpStatus.BAD_REQUEST,"필수 입력값이 누락되었습니다."),
    INVALID_SLICE_VALUE(HttpStatus.BAD_REQUEST, "무한 스크롤을 위한 Slice Page값이 잘못되었습니다. Slice값은 1 ~ N이어야합니다."),
    INVALID_SIZE_VALUE(HttpStatus.BAD_REQUEST, "무한 스크롤을 위한 Size 값이 잘못되었습니다. Size 값은 1 이상이어야 합니다."),

    /**
     * 401 UNAUTHORIZED: 인증되지 않음(로그인 실패 등)
     */
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증되지 않은 사용자입니다."),

    /**
     * 403 FORBIDDEN: 권한 없음 (로그인은 했지만, 해당 리소스에 접근 불가한 경우)
     */
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),

    /**
     * 404 NOT_FOUND: 리소스를 찾을 수 없음
     */
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다."),
    PAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 페이지입니다."),

    /**
     * 405 METHOD_NOT_ALLOWED: 허용되지 않은 Request Method 호출
     */
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "허용되지 않은 메서드입니다."),

    /**
     * 500 INTERNAL_SERVER_ERROR: 내부 서버 오류
     */
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),
    HASHING_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "HashingUtil Class에서 Hashing을 실패했습니다."),
    ;

    private final HttpStatus status;
    private final String message;
}
