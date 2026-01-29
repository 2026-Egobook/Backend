package com.example.egobook_be.domain.notification.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NotificationType {
    LETTER_REQUEST("답장 편지가 도착했어요!"),
    PRAISE("%s 에고북 칭찬서가 도착했어요!"),
    REPORT("저번주 에고북 상담서가 도착했어요!"),
    FRIEND_LETTER("%s 편지가 도착했어요!");

    private final String title;

    public String format(String... args) {
        return String.format(title, args);
    }
}
