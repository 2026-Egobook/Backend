package com.example.egobook_be.domain.user.mapper;

import com.example.egobook_be.domain.auth.entity.AuthAccount;
import com.example.egobook_be.domain.user.dto.AdminUserInfoResDto;
import com.example.egobook_be.domain.user.dto.AdminUserStatsResDto;
import com.example.egobook_be.domain.user.entity.Ability;
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

    public AdminUserStatsResDto toAdminUserStatsResDto(User user, Ability ability, long diaryCount, long letterCount, long letterReplyCount, long questionAnswerCount) {
        return AdminUserStatsResDto.builder()
                .userId(user.getId())
                .activityCount(AdminUserStatsResDto.ActivityCount.builder()
                        .diary(diaryCount)
                        .letter(letterCount)
                        .letterReply(letterReplyCount)
                        .questionAnswer(questionAnswerCount)
                        .build())
                .abilityLevel(AdminUserStatsResDto.AbilityLevel.builder()
                        .empathy(ability.getEmpathy().getLevel())
                        .selfEsteem(ability.getSelfEsteem().getLevel())
                        .emotionRegulation(ability.getEmotionRegulation().getLevel())
                        .positiveThinking(ability.getPositiveThinking().getLevel())
                        .diligence(ability.getDiligence().getLevel())
                        .build())
                .letterReceiveBlockedUntil(user.getLetterReceiveBlockedUntil())
                .notificationEnabled(user.isNotificationEnabled())
                .isFirstAttendanceToday(user.isFirstAttendanceToday())
                .weeklyAnalysisEnabled(user.getWeeklyAnalysisEnabled())
                .counselingTone(user.getCounselingTone())
                .build();
    }
}
