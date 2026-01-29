package com.example.egobook_be.domain.notification.dto;

import lombok.Builder;

@Builder
public record NotificationSettingResDto (
        Boolean enabled
) {}