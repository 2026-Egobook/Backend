package com.example.egobook_be.global.exception;

import com.example.egobook_be.global.exception.model.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

// 전역에서 사용될 에러 코드들을 선언해둔 Enum Class
@Getter
@RequiredArgsConstructor
public enum GlobalErrorCode implements BaseErrorCode {
    /**
     * 400 BAD_REQUEST: 잘못된 요청
     */
    INVALID_INPUT_VALUE("G001", "입력값이 올바르지 않습니다.", HttpStatus.BAD_REQUEST),
    INVALID_TYPE_VALUE("G002", "입력값의 타입이 유효하지 않습니다.", HttpStatus.BAD_REQUEST),
    MISSING_INPUT_VALUE("G003", "필수 입력값이 누락되었습니다.", HttpStatus.BAD_REQUEST),

    /**
     * 401 UNAUTHORIZED: 인증되지 않음(로그인 실패 등)
     */
    UNAUTHORIZED("G004", "인증되지 않은 사용자입니다.", HttpStatus.UNAUTHORIZED),
    TOKEN_EXPIRED("G005", "토큰이 만료되었습니다.", HttpStatus.UNAUTHORIZED),
    INVALID_TOKEN("G006", "유효하지 않은 토큰입니다.", HttpStatus.UNAUTHORIZED),

    /**
     * 403 FORBIDDEN: 권한 없음 (로그인은 했지만, 해당 리소스에 접근 불가한 경우)
     */
    ACCESS_DENIED("G007", "접근 권한이 없습니다.",  HttpStatus.FORBIDDEN),

    /**
     * 404 NOT_FOUND: 리소스를 찾을 수 없음
     */
    RESOURCE_NOT_FOUND("G008", "요청한 리소스를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    PAGE_NOT_FOUND("G009", "존재하지 않는 페이지입니다.", HttpStatus.NOT_FOUND),

    /**
     * 405 METHOD_NOT_ALLOWED: 허용되지 않은 Request Method 호출
     */
    METHOD_NOT_ALLOWED("G010", "허용되지 않은 메서드입니다.", HttpStatus.METHOD_NOT_ALLOWED),

    /**
     * 500 INTERNAL_SERVER_ERROR: 내부 서버 오류
     */
    INTERNAL_SERVER_ERROR("G011", "서버 내부 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String code;
    private final String message;
    private final HttpStatus status;
}
