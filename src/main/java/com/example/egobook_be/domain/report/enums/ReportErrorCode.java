package com.example.egobook_be.domain.report.enums;

import com.example.egobook_be.global.exception.model.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ReportErrorCode implements BaseErrorCode {
    INVALID_REPORT_MEMO_TYPE(HttpStatus.BAD_REQUEST, "유효하지 않은 신고 메모 타입입니다." ),;
    private final HttpStatus status;
    private final String message;
}
