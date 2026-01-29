package com.example.egobook_be.domain.notification.exception;

import com.example.egobook_be.global.exception.model.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum NotificationErrorCode implements BaseErrorCode {

    NOTIFICATION_ACCESS_DENIED(
            "NOTE_403_001",
            "해당 알림에 접근할 권한이 없습니다.",
            HttpStatus.FORBIDDEN
    ),

    USER_NOT_FOUND(
            "NOTE_404_001",
            "해당 사용자를 찾을 수 없습니다.",
            HttpStatus.NOT_FOUND
    ),

    NOTIFICATION_NOT_FOUND(
            "NOTE_404_002",
            "알림을 찾을 수 없습니다.",
            HttpStatus.NOT_FOUND
    ),

    ;

    private final String code;
    private final String message;
    private final HttpStatus status;
}
