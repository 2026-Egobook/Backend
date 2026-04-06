package com.example.egobook_be.domain.user.exception;

import com.example.egobook_be.global.exception.model.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AdminUserErrorCode implements BaseErrorCode {
    KEYWORD_IS_NULL_OR_BLANK(HttpStatus.BAD_REQUEST, "검색 키워드가 비었습니다.");


    private final HttpStatus httpStatus;
    private final String message;

    @Override
    public HttpStatus getStatus() {
        return this.httpStatus;
    }
}
