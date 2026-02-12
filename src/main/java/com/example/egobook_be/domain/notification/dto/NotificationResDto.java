package com.example.egobook_be.domain.notification.dto;

import com.example.egobook_be.domain.notification.enums.NotificationType;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record NotificationResDto(
        Long notificationId,
        NotificationType type,
        String title,
        String content,
        Boolean isRead,
        Long targetId,
        LocalDateTime createdAt
) {}