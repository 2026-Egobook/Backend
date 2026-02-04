package com.example.egobook_be.domain.notification.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NotificationType {
    LETTER_REPLY("답장 편지가 도착했어요!"),
    LETTER_REPLY_FRIEND("%s님 답장 편지가 도착했어요!"),
    LETTER_NEW("새로운 편지가 도착했어요!"),
    LETTER_NEW_FRIEND("새로운 %s님 편지가 도착했어요!"),
    PRAISE("%s 일간 칭찬서가 도착했어요!"),
    REPORT("지난주 주간 리포트가 도착했어요!");

    private final String title;

    public String format(String... args) {
        return String.format(title, args);
    }
}
