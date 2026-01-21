package com.example.egobook_be.domain.notification.exception;

import com.example.egobook_be.global.exception.model.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum NotificationErrorCode implements BaseErrorCode {

    USER_NOT_FOUND(
            "NOTE_404_001",
            "해당 사용자를 찾을 수 없습니다.",
            HttpStatus.NOT_FOUND
    ),
    ;

    private final String code;
    private final String message;
    private final HttpStatus status;
}
