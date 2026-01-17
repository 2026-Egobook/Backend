package com.example.egobook_be.global.util.module;

import com.example.egobook_be.domain.user.entity.RoleType;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Redis에 저장할 Refresh Token Value 구조체
 * @param subject : "provider:hashedDeviceUid"
 * @param role : ROLE_USER
 * @param expiresAt : Refresh Token 만료 절대 시간
 */
@Builder
public record RedisValue(
        String subject,
        RoleType role,
        LocalDateTime expiresAt
) {
}
