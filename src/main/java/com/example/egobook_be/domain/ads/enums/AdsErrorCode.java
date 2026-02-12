package com.example.egobook_be.domain.ads.enums;

import com.example.egobook_be.global.exception.model.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AdsErrorCode implements BaseErrorCode {
    /** 400 */
    UNDEFINED_AD_REWARD_TYPE(HttpStatus.BAD_REQUEST, "정의되지 않은 보상 타입입니다."),
    EXCEED_DAILY_ADS_NUM(HttpStatus.BAD_REQUEST, "하루 광고 시청 횟수를 초과하였습니다."),

    /** 409 */
    TRANSACTION_ID_ALREADY_EXIST(HttpStatus.CONFLICT, "광고의 고유한 Transaction ID가 이미 DB에 존재합니다.");

    private final HttpStatus status;
    private final String message;
}
