package com.example.egobook_be.domain.coupon.exception;

import com.example.egobook_be.global.exception.model.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CouponErrorCode implements BaseErrorCode {

    COUPON_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 코드입니다"),
    COUPON_EXPIRED(HttpStatus.BAD_REQUEST, "만료된 쿠폰입니다"),
    COUPON_ALREADY_USED(HttpStatus.BAD_REQUEST, "이미 사용한 쿠폰입니다"),
    COUPON_NOT_FOR_USER(HttpStatus.FORBIDDEN, "해당 쿠폰을 사용할 수 없습니다"),

    // Admin
    DUPLICATED_CODE(HttpStatus.CONFLICT, "중복된 코드입니다"),
    REWARD_REQUIRED(HttpStatus.BAD_REQUEST, "보상은 1개 이상 등록해야 합니다"),
    INVALID_REWARD(HttpStatus.BAD_REQUEST, "보상 정보가 올바르지 않습니다"),
    REWARD_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "보상 아이템을 찾을 수 없습니다"),
    TARGET_ACCOUNT_CODE_REQUIRED(HttpStatus.BAD_REQUEST, "개인 대상 쿠폰은 계정 고유 코드가 필요합니다"),
    TARGET_ACCOUNT_CODE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "전체 대상 쿠폰에는 계정 고유 코드를 지정할 수 없습니다"),
    TARGET_USER_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 계정 고유 코드의 사용자를 찾을 수 없습니다"),
    COUPON_ALREADY_USED_CANNOT_DELETE(HttpStatus.CONFLICT, "이미 사용된 쿠폰은 삭제할 수 없습니다"),
    USED_COUPON_LIMITED_UPDATE(HttpStatus.CONFLICT, "이미 사용된 쿠폰은 만료일만 수정할 수 있습니다"),
    NOTIFY_TARGET_NOT_INDIVIDUAL(HttpStatus.BAD_REQUEST, "개인 대상 쿠폰만 알림을 전송할 수 있습니다"),
    COUPON_ALREADY_NOTIFIED(HttpStatus.CONFLICT, "이미 알림을 전송한 쿠폰입니다"),
    RECEIVER_NOTIFICATION_DISABLED(HttpStatus.BAD_REQUEST, "대상 유저가 알림을 꺼두어 전송할 수 없습니다");

    private final HttpStatus httpStatus;
    private final String message;

    @Override
    public HttpStatus getStatus() {
        return this.httpStatus;
    }
}