package com.example.egobook_be.domain.ego_room.exception;

import com.example.egobook_be.global.exception.CustomException;
import com.example.egobook_be.global.exception.model.BaseErrorCode;
import lombok.Getter;
import java.util.Map;

@Getter
public class SubscriptionLockedException extends CustomException {
    private final String dynamicCode; // "SUB403_WEEKLY_LOCKED" 같은 유동적 코드
    private final Map<String, Object> result;

    public SubscriptionLockedException(BaseErrorCode errorCode, String dynamicCode, String target) {
        super(errorCode); // 부모 CustomException에 에러 코드 전달
        this.dynamicCode = dynamicCode;
        this.result = Map.of("cta", Map.of(
                "label", "구독하러가기",
                "target", target
        ));
    }
}