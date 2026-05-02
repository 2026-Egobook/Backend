package com.example.egobook_be.domain.admin.exception;

import com.example.egobook_be.global.exception.model.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AdminAuthErrorCode implements BaseErrorCode {
    ADMIN_ID_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 존재하는 ID입니다."),
    ADMIN_LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "인증에 실패했습니다."),
    ADMIN_NOT_APPROVED(HttpStatus.FORBIDDEN, "아직 관리자 계정이 승인되지 않았습니다."),
    ADMIN_SIGNUP_REJECTED(HttpStatus.FORBIDDEN, "관리자 계정 회원가입 요청이 거절되었습니다."),
    ADMIN_REFRESH_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "Redis에 Refresh Token 정보가 없습니다. 관리자 계정으로 다시 로그인하세요."),
    INVALID_ADMIN_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 관리자 Refresh Token입니다.");

    private final HttpStatus status;
    private final String message;
}
