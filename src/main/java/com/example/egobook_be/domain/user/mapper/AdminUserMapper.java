package com.example.egobook_be.domain.user.mapper;

import com.example.egobook_be.domain.auth.entity.AuthAccount;
import com.example.egobook_be.domain.letters.entity.PlazaLetter;
import com.example.egobook_be.domain.letters.entity.PlazaLetterReply;
import com.example.egobook_be.domain.letters.entity.PlazaLetterReplyReport;
import com.example.egobook_be.domain.letters.entity.PlazaLetterReport;
import com.example.egobook_be.domain.question.entity.AnswerReport;
import com.example.egobook_be.domain.question.entity.QuestionAnswer;
import com.example.egobook_be.domain.user.dto.AdminUserInfoResDto;
import com.example.egobook_be.domain.user.dto.AdminUserReportHistoryResDto;
import com.example.egobook_be.domain.user.dto.AdminUserStatsResDto;
import com.example.egobook_be.domain.user.entity.Ability;
import com.example.egobook_be.domain.user.entity.User;
import com.example.egobook_be.global.enums.ReportDomainType;
import com.example.egobook_be.global.enums.ReportType;
import com.example.egobook_be.global.response.SliceResponse;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Component;

@Component
public class AdminUserMapper {
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

    public AdminUserReportHistoryResDto toAdminUserReportHistoryResDto(AdminUserReportHistoryResDto.Summary summary, SliceResponse<AdminUserReportHistoryResDto.ReportContent> reportList) {
        return AdminUserReportHistoryResDto.builder()
                .summary(summary)
                .reportList(reportList)
                .build();
    }

    public AdminUserReportHistoryResDto.ReportContent toReportContent(PlazaLetterReport report, PlazaLetter letter, ReportDomainType reportDomainType, ReportType reportType) {
        return AdminUserReportHistoryResDto.ReportContent.builder()
                .reportId(report.getReportId())
                .reportDomainType(reportDomainType)
                .reportType(reportType)
                .reportReason(report.getReason())
                .reportStatus(report.getStatus())
                .createdAt(report.getCreatedAt())
                .targetId(letter.getLetterId())
                .content(letter.getContent())
                .build();
    }

    public AdminUserReportHistoryResDto.ReportContent toReportContentFromLetterReply(
            PlazaLetterReplyReport report, PlazaLetterReply letterReply,
            ReportDomainType reportDomainType, ReportType reportType
    ) {
        return AdminUserReportHistoryResDto.ReportContent.builder()
                .reportId(report.getReportId())
                .reportDomainType(reportDomainType)
                .reportType(reportType)
                .reportReason(report.getReason())
                .reportStatus(report.getStatus())
                .createdAt(report.getCreatedAt())
                .targetId(letterReply.getReplyId())
                .content(letterReply.getContent())
                .build();
    }

    public AdminUserReportHistoryResDto.ReportContent toReportContentFromQuestionAnswer(
            AnswerReport report, QuestionAnswer questionAnswer,
            ReportDomainType reportDomainType, ReportType reportType
    ) {
        return AdminUserReportHistoryResDto.ReportContent.builder()
                .reportId(report.getId())
                .reportDomainType(reportDomainType)
                .reportType(reportType)
                .reportReason(report.getReason())
                .reportStatus(report.getStatus())
                .createdAt(report.getCreatedAt())
                .targetId(questionAnswer.getId())
                .content(questionAnswer.getContent())
                .build();
    }
}
