package com.example.egobook_be.domain.user.entity;

import com.example.egobook_be.domain.user.enums.UserStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    @Test
    @DisplayName("탈퇴와 복구 시 기존 수신 설정 유지")
    void 탈퇴와_복구_시_기존_수신_설정_유지_성공() {
        // given
        User user = User.builder()
                .status(UserStatus.ACTIVE)
                .dailyPraise(false)
                .weeklyAnalysisEnabled(false)
                .notificationEnabled(false)
                .build();

        // when
        user.withdrawUser(86_400_000L);
        user.cancelWithDrawUser();

        // then
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(user.getDeletedAt()).isNull();
        assertThat(user.getPurgeAt()).isNull();
        assertThat(user.getDailyPraise()).isFalse();
        assertThat(user.getWeeklyAnalysisEnabled()).isFalse();
        assertThat(user.isNotificationEnabled()).isFalse();
    }
}
