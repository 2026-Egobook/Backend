package com.example.egobook_be.domain.friend.exception;

import com.example.egobook_be.global.exception.model.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum FriendErrorCode implements BaseErrorCode {

    /**
     * 404 NOT_FOUND
     */
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 사용자를 찾을 수 없습니다."),
    FRIEND_REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND, "친구 신청 내역을 찾을 수 없습니다."),

    /**
     * 409 CONFLICT
     */
    ALREADY_FRIEND(HttpStatus.CONFLICT, "이미 친구 관계입니다."),
    ALREADY_REQUESTED(HttpStatus.CONFLICT, "이미 친구 신청을 보낸 상태입니다."),
    FRIEND_LIMIT_EXCEEDED(HttpStatus.CONFLICT, "친구는 최대 10명까지만 추가할 수 있습니다."),
    FRIEND_REQUEST_CONFLICT(HttpStatus.CONFLICT, "이미 처리된 친구 요청입니다."),

    /**
     * 400 BAD_REQUEST
     */
    SELF_REQUEST_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "자기 자신에게 친구 신청을 보낼 수 없습니다."),
    INVALID_FRIEND_REQUEST_STATE(HttpStatus.BAD_REQUEST, "이미 처리된 친구 신청입니다.");

    private final HttpStatus status;
    private final String message;
}
