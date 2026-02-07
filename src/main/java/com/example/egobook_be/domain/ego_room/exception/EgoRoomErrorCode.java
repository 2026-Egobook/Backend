package com.example.egobook_be.domain.ego_room.exception;

import com.example.egobook_be.global.exception.model.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.boot.autoconfigure.graphql.GraphQlProperties;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum EgoRoomErrorCode implements BaseErrorCode {


    COUNSEL_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 주간 상담서를 찾을 수 없습니다."),
    INSUFFICIENT_INK(HttpStatus.PAYMENT_REQUIRED, "잉크가 부족합니다. 잉크를 충전하거나 광고를 시청해주세요."),
    STATS_DATA_NOT_FOUND(HttpStatus.BAD_REQUEST, "첫 일기를 작성한 후 24시간이 지나야 통계 조회가 가능합니다.");

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