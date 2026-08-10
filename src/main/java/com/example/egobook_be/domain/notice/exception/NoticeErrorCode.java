package com.example.egobook_be.domain.notice.exception;

import com.example.egobook_be.global.exception.model.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum NoticeErrorCode implements BaseErrorCode {

    NOTICE_NOT_FOUND(HttpStatus.NOT_FOUND, "공지사항을 찾을 수 없습니다."),

    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 사용자를 찾을 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String message;

    @Override
    public HttpStatus getStatus() {
        return this.httpStatus;
    }
}
