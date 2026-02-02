package com.example.egobook_be.domain.ego_room.exception;

import com.example.egobook_be.global.exception.model.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.boot.autoconfigure.graphql.GraphQlProperties;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum EgoRoomErrorCode implements BaseErrorCode {


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