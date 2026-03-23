package com.example.egobook_be.domain.user.enums;

import com.example.egobook_be.global.exception.model.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements BaseErrorCode {
    // 400 BAD REQUEST
    WITHDRAW_REASON_OTHER_TEXT_FIELD_EMPTY(HttpStatus.BAD_REQUEST, "회원탈퇴 이유가 '기타'일 때 상세 이유는 반드시 적어야합니다."),

    // 404 NOT FOUND
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 사용자를 찾지 못했습니다."),
    ABILITY_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 사용자의 능력치 정보를 찾지 못했습니다."),
    MISSION_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 사용자의 미션 상태 정보를 찾지 못했습니다."),

    // 409 CONFLICT
    ALREADY_WITHDRAW_PENDING(HttpStatus.CONFLICT, "해당 사용자는 이미 탈퇴 대기 상태입니다."),
    WITHDRAW_REASON_ALREADY_WRITTEN(HttpStatus.CONFLICT, "해당 사용자는 이미 탈퇴 이유를 제출했습니다.");


    private final HttpStatus status;
    private final String message;
}
