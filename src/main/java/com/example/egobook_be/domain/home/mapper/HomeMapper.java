package com.example.egobook_be.domain.home.mapper;

import com.example.egobook_be.domain.home.dto.HomeResDto;
import com.example.egobook_be.domain.user.entity.User;
import org.springframework.stereotype.Component;

@Component
public class HomeMapper {

    public HomeResDto toHomeResDto(User user, Integer unReadNotificationCount, Integer unopenedPsychology, Integer attendanceRewardInk) {
        return HomeResDto.builder()
                .nickname(user.getNickname())
                .level(user.getLevel())
                .ink(user.getInk())
                .unreadNotifications(unReadNotificationCount)
                .unopenedPsychology(unopenedPsychology)
                .isFirstAttendanceToday(user.isFirstAttendanceToday())
                .attendanceRewardInk(attendanceRewardInk)
                .build();
    }
}
