package com.example.egobook_be.domain.home.service;

import com.example.egobook_be.domain.home.dto.HomeResDto;
import org.springframework.stereotype.Service;

@Service
public class HomeService {

    public HomeResDto getHomeData(Long userId){
        return HomeResDto.builder()
                .nickname("에고북1234")
                .level(1)
                .ink(100)
                .hasUnreadNotifications(true)
                .hasUnopenedPsychology(true)
                .isFirstAttendanceToday(true)
                .attendanceRewardInk(3)
                .build();
    }
}
