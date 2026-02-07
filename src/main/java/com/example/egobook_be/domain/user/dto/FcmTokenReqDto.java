package com.example.egobook_be.domain.user.dto;

import lombok.Builder;

@Builder
public record FcmTokenReqDto (
        String fcmToken
) {}
