package com.example.egobook_be.domain.user.dto;

import com.example.egobook_be.domain.user.enums.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "회원 검색 시 회원 정보 DTO")
public record SearchUserResDto(
        Long userId,
        String accountCode,
        String email,
        String nickname,
        UserStatus status
) {
}
