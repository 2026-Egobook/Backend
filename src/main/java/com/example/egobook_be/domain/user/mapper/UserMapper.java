package com.example.egobook_be.domain.user.mapper;

import com.example.egobook_be.domain.auth.entity.AuthAccount;
import com.example.egobook_be.domain.user.dto.AdminUserInfoResDto;
import com.example.egobook_be.domain.user.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {
    public AdminUserInfoResDto toAdminUserInfoResDto(User user, AuthAccount authAccount) {
        return AdminUserInfoResDto.builder()
                .userId(user.getId())
                .accountCode(user.getAccountCode())
                .email(user.getEmail())
                .provider(authAccount.getProvider())
                .nickname(user.getNickname())
                .createdAt(user.getCreatedAt())
                .level(user.getLevel())
                .ink(user.getInk())
                .lastLoginAt(user.getLastLoginAt())
                .status(user.getStatus())
                .deletedAt(user.getDeletedAt())
                .purgeAt(user.getPurgeAt())
                .build();
    }
}
