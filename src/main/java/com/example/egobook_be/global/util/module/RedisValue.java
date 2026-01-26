package com.example.egobook_be.global.util.module;

import com.example.egobook_be.domain.user.enums.RoleType;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * Redis에 저장할 Refresh Token Value 구조체
 * @param userId : User PK
 * @param authAccountId : AuthAccount PK
 * @param subject : "provider:hashedDeviceUid"
 * @param role : ROLE_USER
 * @param expiresAt : Refresh Token 만료 절대 시간
 */
@Builder
public record RedisValue(
        Long userId,
        Long authAccountId,
        String subject,
        RoleType role,
        LocalDateTime expiresAt
) {
}
