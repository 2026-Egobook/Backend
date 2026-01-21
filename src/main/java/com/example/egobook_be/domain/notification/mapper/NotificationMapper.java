package com.example.egobook_be.domain.notification.mapper;

import com.example.egobook_be.domain.notification.dto.NotificationResDto;
import com.example.egobook_be.domain.notification.entity.Notification;

public class NotificationMapper {
    public static NotificationResDto toNotificationDto(Notification notification) {
        return NotificationResDto.builder()
                .title(notification.getTitle())
                .type(notification.getType())
                .content(notification.getContent())
                .isRead(notification.isRead())
                .targetId(notification.getTargetId())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
