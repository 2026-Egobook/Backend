package com.example.egobook_be.global.util.module;

import lombok.Builder;

import java.time.LocalDateTime;

/**
 * JwtUtil의 createToken() 함수에서 반환할 값
 * 토큰과 토큰의 expiresAt 값을 담고 있다.
 * @param token : 발급한 token
 * @param expiresAt : 발급한 token의 만료 시간
 */
@Builder
public record TokenInfo(
        String token,

        LocalDateTime expiresAt
) {
}
