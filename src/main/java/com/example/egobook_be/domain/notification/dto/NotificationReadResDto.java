package com.example.egobook_be.domain.notification.dto;

import lombok.Builder;

@Builder
public record NotificationReadResDto(
        Long notificationId,
        boolean isRead
) {}
