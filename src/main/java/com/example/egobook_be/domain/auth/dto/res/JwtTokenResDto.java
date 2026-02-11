package com.example.egobook_be.domain.auth.dto.res;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

/**
 * 로그인/회원가입을 했을 때 클라이언트에게 내려줄 토큰들
 */
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL) // 값이 null인 필드는 json 응답에서 제외하는 어노테이션
public record JwtTokenResDto(
        String accessToken,
        String refreshToken,
        String recoverToken, // 회원가입 시에만 값 존재하며, 로그인 시에는 null
        String email
) {
}
