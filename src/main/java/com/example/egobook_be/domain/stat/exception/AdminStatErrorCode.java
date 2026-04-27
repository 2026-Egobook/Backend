package com.example.egobook_be.domain.stat.exception;

import com.example.egobook_be.global.exception.model.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AdminStatErrorCode implements BaseErrorCode {

    INVALID_STAT_DATE(
            "STAT_400_001",
            "유효하지 않은 날짜 범위입니다.",
            HttpStatus.BAD_REQUEST
    ),
    DATE_RANGE_TOO_LONG(
            "STAT_400_002",
            "조회 범위를 초과했습니다. (최대 90일)",
            HttpStatus.BAD_REQUEST
    ),
    ;

    private final String code;
    private final String message;
    private final HttpStatus status;
}
