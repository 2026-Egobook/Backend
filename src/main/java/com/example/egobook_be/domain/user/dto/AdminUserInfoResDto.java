package com.example.egobook_be.domain.user.dto;

import com.example.egobook_be.domain.auth.enums.Provider;
import com.example.egobook_be.domain.user.entity.User;
import com.example.egobook_be.domain.user.enums.RoleType;
import com.example.egobook_be.domain.user.enums.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;

@Schema(description = "관리자 회원 기본 정보 조회 DTO")
@Builder
public record AdminUserInfoResDto(
        @Schema(description = "사용자 ID") Long userId,
        @Schema(description = "계정 코드") String accountCode,
        @Schema(description = "이메일") String email,
        @Schema(description = "가입 유형") Provider provider,
        @Schema(description = "닉네임") String nickname,
        @Schema(description = "가입 일시") LocalDateTime createdAt,
        @Schema(description = "레벨") Integer level,
        @Schema(description = "보유 잉크") Integer ink,
        @Schema(description = "마지막 로그인 시각") LocalDateTime lastLoginAt,
        @Schema(description = "계정 상태") UserStatus status,
        @Schema(description = "탈퇴 신청 일시") LocalDateTime deletedAt,
        @Schema(description = "사용자 정보 삭제 일시") LocalDateTime purgeAt
) {
}
