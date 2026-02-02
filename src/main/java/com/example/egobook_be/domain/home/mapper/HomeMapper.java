package com.example.egobook_be.domain.home.mapper;

import com.example.egobook_be.domain.home.dto.HomeAbilityResDto;
import com.example.egobook_be.domain.home.dto.HomeActivityResDto;
import com.example.egobook_be.domain.home.dto.HomeResDto;
import com.example.egobook_be.domain.home.dto.HomeSettingResDto;
import com.example.egobook_be.domain.home.entity.Mission;
import com.example.egobook_be.domain.user.entity.Ability;
import com.example.egobook_be.domain.user.entity.AbilityStat;
import com.example.egobook_be.domain.user.entity.User;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class HomeMapper {

    public HomeResDto toHomeResDto(User user, Integer unReadNotificationCount, Boolean hasUnopenedPsychology, Integer attendanceRewardInk) {
        return HomeResDto.builder()
                .nickname(user.getNickname())
                .level(user.getLevel())
                .ink(user.getInk())
                .unreadNotifications(unReadNotificationCount)
                .hasUnopenedPsychology(hasUnopenedPsychology)
                .isFirstAttendanceToday(user.isFirstAttendanceToday())
                .attendanceRewardInk(attendanceRewardInk)
                .build();
    }

    /**
     * 활동 목록 정보 변환 (미션 데이터가 존재할 때)
     */
    public HomeActivityResDto toHomeActivityResDto(Mission mission) {
        // 요일별 수행 여부를 리스트로 변환
        List<Boolean> weeklyStatus = List.of(
                mission.isMondayCompleted(),
                mission.isTuesdayCompleted(),
                mission.isWednesdayCompleted(),
                mission.isThursdayCompleted(),
                mission.isFridayCompleted(),
                mission.isSaturdayCompleted(),
                mission.isSundayCompleted()
        );

        return HomeActivityResDto.builder()
                .isDailyMissionSuccess(mission.isDailyMissionSuccess())
                .hasWrittenDiary(mission.isDailyDiaryWritten())
                .hasWrittenLetter(mission.isDailyLetterWritten())
                .hasAnsweredQuestion(mission.isDailyQuestionAnswered())
                .consecutiveWeeks(mission.getConsecutiveWeeks())
                .weeklyMissionStatus(weeklyStatus)
                .build();
    }

    /**
     * 활동 목록 정보 빈 객체 반환 (미션 데이터가 없을 때)
     */
    public HomeActivityResDto toEmptyHomeActivityResDto() {
        return HomeActivityResDto.builder()
                .isDailyMissionSuccess(false)
                .hasWrittenDiary(false)
                .hasWrittenLetter(false)
                .hasAnsweredQuestion(false)
                .consecutiveWeeks(0)
                .weeklyMissionStatus(List.of(false, false, false, false, false, false, false))
                .build();
    }

    public HomeAbilityResDto toHomeAbilityResDto(Ability ability) {
        AbilityStat empathy = ability.getEmpathy();
        AbilityStat selfEsteem = ability.getSelfEsteem();
        AbilityStat emotionRegulation = ability.getEmotionRegulation();
        AbilityStat positiveThinking = ability.getPositiveThinking();
        AbilityStat diligence = ability.getDiligence();

        return HomeAbilityResDto.builder()
                .empathy(toAbilityInfo(empathy))
                .selfEsteem(toAbilityInfo(selfEsteem))
                .emotionRegulation(toAbilityInfo(emotionRegulation))
                .positiveThinking(toAbilityInfo(positiveThinking))
                .diligence(toAbilityInfo(diligence))
                .build();
    }

    private HomeAbilityResDto.AbilityInfo toAbilityInfo(AbilityStat abilityStat) {
        return HomeAbilityResDto.AbilityInfo.builder()
                .level(abilityStat.getLevel())
                .score(abilityStat.getScore())
                .color(abilityStat.getColor())
                .build();
    }

    public HomeSettingResDto toHomeSettingResDto(User user) {
        return HomeSettingResDto.builder()
                .accountCode(user.getAccountCode())
                .build();
    }

}
