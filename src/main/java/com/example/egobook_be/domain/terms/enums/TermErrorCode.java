package com.example.egobook_be.domain.terms.enums;

import com.example.egobook_be.global.exception.model.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum TermErrorCode implements BaseErrorCode {
    /**
     * 404 NOT_FOUND
     */
    TERMS_NOT_FOUND(HttpStatus.NOT_FOUND, "약관들을 찾지 못했습니다.");

    private final HttpStatus status;
    private final String message;
}
