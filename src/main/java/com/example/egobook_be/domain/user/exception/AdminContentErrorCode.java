package com.example.egobook_be.domain.user.exception;

import com.example.egobook_be.global.exception.model.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AdminContentErrorCode implements BaseErrorCode {

    INVALID_DATE_RANGE(HttpStatus.BAD_REQUEST, "조회 시작일은 종료일보다 이전이어야 합니다."),
    FAIL_IDS_EMPTY(HttpStatus.BAD_REQUEST, "재발송 대상 ID 목록이 비어 있습니다."),
    FAIL_LOG_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 실패 로그를 찾을 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String message;

    @Override
    public HttpStatus getStatus() {
        return this.httpStatus;
    }
}