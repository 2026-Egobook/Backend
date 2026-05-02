package com.example.egobook_be.domain.shop.enums;

import com.example.egobook_be.global.exception.model.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ShopErrorCode implements BaseErrorCode {
    /*
     * 400
     */
    INSUFFICIENT_INK_TO_BUY_ITEM(HttpStatus.BAD_REQUEST, "잉크가 부족하여 해당 아이템을 구매할 수 없습니다."),
    ITEM_NOT_PURCHASED(HttpStatus.BAD_REQUEST, "아이템이 구매되지 않았습니다."),
    INVALID_ITEM_STATUS(HttpStatus.BAD_REQUEST,"유효하지 않은 상태 값입니다."),
    /*
     * 404 NOT FOUND
     */
    ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 아이템을 찾을 수 없습니다."),
    DEFAULT_ITEMS_NOT_FOUND(HttpStatus.NOT_FOUND, "기본 아이템들을 찾을 수 없습니다."),
    FILE_NOT_FOUND_IN_S3(HttpStatus.NOT_FOUND, "S3에 해당 파일이 존재하지 않습니다."),
    /*
     * 409 CONFLICT (충돌)
     */
    ALREADY_PURCHASED_ITEM(HttpStatus.CONFLICT, "이미 구매된 아이템입니다."),
    ALREADY_EXIST_ITEM(HttpStatus.CONFLICT,"이미 같은 경로/파일명이 존재합니다.");
    private final HttpStatus status;
    private final String message;
}
