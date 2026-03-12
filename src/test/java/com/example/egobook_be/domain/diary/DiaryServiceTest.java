package com.example.egobook_be.domain.diary;

import com.example.egobook_be.domain.diary.dto.DiaryCreateReqDto;
import com.example.egobook_be.domain.diary.dto.DiaryCreateResDto;
import com.example.egobook_be.domain.diary.entity.Diary;
import com.example.egobook_be.domain.diary.enums.DiaryType;
import com.example.egobook_be.domain.diary.enums.RewardType;
import com.example.egobook_be.domain.diary.repository.DiaryRepository;
import com.example.egobook_be.domain.diary.service.DiaryQueryService;
import com.example.egobook_be.domain.diary.service.DiaryService;
import com.example.egobook_be.domain.home.entity.Mission;
import com.example.egobook_be.domain.user.entity.Ability;
import com.example.egobook_be.domain.user.entity.AbilityStat;
import com.example.egobook_be.domain.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DiaryServiceTest {
    @InjectMocks
    private DiaryService diaryService;

    @Mock
    private DiaryRepository diaryRepository;

    @Mock
    private DiaryQueryService diaryQueryService;

    @Mock private User user;
    @Mock private Ability ability;
    @Mock private Mission mission;

    @Test
    @DisplayName("고민 일기 작성 시 레벨업이 발생하면 추가 잉크 보상을 획득한다")
    void createDiary_ShouldRewardInk_WhenEmotionRegulationLevelsUp() {
        // Given
        Long userId = 1L;
        DiaryCreateReqDto reqDto = new DiaryCreateReqDto(
                Set.of(DiaryType.EMOTION, DiaryType.CONCERN),
                3,
                "오늘 감정 조절을 잘했다.",
                LocalDate.now()
        );

        given(diaryQueryService.getUserById(userId)).willReturn(user);
        given(diaryQueryService.getAbilityByUser(user)).willReturn(ability);
        given(diaryQueryService.getMissionByUser(user)).willReturn(mission);

        Diary savedDiary = Diary.builder()
                .id(1L)
                .user(user)
                .date(LocalDate.now())
                .type(reqDto.type())
                .content(reqDto.content())
                .writtenAt(LocalDateTime.now())
                .build();
        given(diaryRepository.save(any(Diary.class))).willReturn(savedDiary);

        given(diaryRepository.countByUserAndDate(any(), any())).willReturn(0);

        // [핵심 해결책]
        // "이미 오늘 쓴 일기가 존재한다(true)"고 설정 -> isFirstDiaryToday = false
        // 이렇게 하면 '첫 일기 보상' 로직을 건너뛰고 '레벨업 보상'만 테스트할 수 있습니다.
        given(diaryRepository.existsByUserAndCreatedAtBetween(
                any(User.class), any(LocalDateTime.class), any(LocalDateTime.class)
        )).willReturn(true);

        given(diaryRepository.existsByUserAndTypeContainingAndCreatedAtBetween(
                any(User.class), eq(DiaryType.CONCERN), any(LocalDateTime.class), any(LocalDateTime.class)
        )).willReturn(false);

        // 레벨업 시뮬레이션 (잉크 1 획득)
        given(ability.addEmotionRegulation(1)).willReturn(1);

        AbilityStat level2Stat = AbilityStat.builder().level(2).score(0).build();
        given(ability.getEmotionRegulation()).willReturn(level2Stat);

        // When
        DiaryCreateResDto result = diaryService.createDiary(userId, reqDto);

        // Then
        // 이제 첫 일기 보상이 차단되었으므로, 레벨업 보상 1회만 호출되어야 함
        verify(user).addInk(1);

        List<DiaryCreateResDto.RewardResDto> rewards = result.rewards();
        assertThat(rewards).hasSizeGreaterThanOrEqualTo(2);

        boolean hasLevelUpReward = rewards.stream()
                .anyMatch(r -> r.message().contains("레벨업")
                        && r.rewardType() == RewardType.INK
                        && r.amount() == 1);
        assertThat(hasLevelUpReward).isTrue();
    }

    @Test
    @DisplayName("고민 일기 작성 시 레벨업하지 않으면 점수 상승 보상만 획득한다")
    void createDiary_ShouldNotRewardExtraInk_WhenEmotionRegulationDoesNotLevelUp() {
        // Given
        Long userId = 1L;
        DiaryCreateReqDto reqDto = new DiaryCreateReqDto(
                Set.of(DiaryType.EMOTION, DiaryType.CONCERN),
                3,
                "평범한 고민 일기",
                LocalDate.now()
        );

        given(diaryQueryService.getUserById(userId)).willReturn(user);
        given(diaryQueryService.getAbilityByUser(user)).willReturn(ability);
        given(diaryQueryService.getMissionByUser(user)).willReturn(mission);

        Diary savedDiary = Diary.builder()
                .id(1L)
                .user(user)
                .date(LocalDate.now())
                .type(reqDto.type())
                .content(reqDto.content())
                .writtenAt(LocalDateTime.now())
                .build();
        given(diaryRepository.save(any(Diary.class))).willReturn(savedDiary);
        given(diaryRepository.countByUserAndDate(any(), any())).willReturn(0);

        // [핵심 해결책]
        // "이미 오늘 쓴 일기가 존재한다(true)"고 설정 -> isFirstDiaryToday = false
        // 첫 일기 보상 로직 차단
        given(diaryRepository.existsByUserAndCreatedAtBetween(
                any(User.class), any(LocalDateTime.class), any(LocalDateTime.class)
        )).willReturn(true);

        given(diaryRepository.existsByUserAndTypeContainingAndCreatedAtBetween(
                any(), eq(DiaryType.CONCERN), any(), any()
        )).willReturn(false);

        // 레벨업 하지 않음 (잉크 0 획득)
        given(ability.addEmotionRegulation(1)).willReturn(0);

        // When
        DiaryCreateResDto result = diaryService.createDiary(userId, reqDto);

        // Then
        // 첫 일기 보상도 없고, 레벨업 보상도 없으므로 호출되지 않아야 함
        verify(user, never()).addInk(1);

        List<DiaryCreateResDto.RewardResDto> rewards = result.rewards();
        boolean hasLevelUpReward = rewards.stream()
                .anyMatch(r -> r.message().contains("레벨업"));
        assertThat(hasLevelUpReward).isFalse();

        boolean hasScoreReward = rewards.stream()
                .anyMatch(r -> r.message().contains("스코어가 상승"));
        assertThat(hasScoreReward).isTrue();
    }
}