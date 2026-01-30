package com.example.egobook_be.domain.ego_room.exception;

import com.example.egobook_be.global.exception.model.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum EgoRoomErrorCode implements BaseErrorCode {

    WEEKLY_COUNSEL_LOCKED(HttpStatus.FORBIDDEN, "월정액 미구독 시 이용 불가");

    private final HttpStatus status;
    private final String message;

    @Override
    public HttpStatus getStatus() {
        return this.status;
    }

    @Override
    public String getMessage() {
        return this.message;
    }
}