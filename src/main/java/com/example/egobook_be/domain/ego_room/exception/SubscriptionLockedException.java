package com.example.egobook_be.domain.ego_room.exception;

import lombok.Getter;
import java.util.Map;

@Getter
public class SubscriptionLockedException extends RuntimeException {
    private final String code = "SUB403_STATS_LOCKED";
    private final String message = "월정액 미구독 시 이용 불가";
    private final Map<String, Object> result;

    public SubscriptionLockedException() {
        this.result = Map.of("cta", Map.of(
                "label", "구독하러가기",
                "target", "B1.3_SETTINGS"
        ));
    }
}