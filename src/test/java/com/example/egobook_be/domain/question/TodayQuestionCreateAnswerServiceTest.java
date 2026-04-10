package com.example.egobook_be.domain.question;

import com.example.egobook_be.domain.home.entity.Mission;
import com.example.egobook_be.domain.home.repository.MissionRepository;
import com.example.egobook_be.domain.question.dto.AnswerCreateReqDto;
import com.example.egobook_be.domain.question.entity.QuestionAnswer;
import com.example.egobook_be.domain.question.entity.TodayQuestion;
import com.example.egobook_be.domain.question.enums.AnswerVisibility;
import com.example.egobook_be.domain.question.exception.QuestionErrorCode;
import com.example.egobook_be.domain.question.repository.QuestionAnswerRepository;
import com.example.egobook_be.domain.question.repository.TodayQuestionRepository;
import com.example.egobook_be.domain.question.service.TodayQuestionService;
import com.example.egobook_be.domain.user.entity.Ability;
import com.example.egobook_be.domain.user.entity.User;
import com.example.egobook_be.domain.user.exception.UserErrorCode;
import com.example.egobook_be.domain.user.repository.AbilityRepository;
import com.example.egobook_be.domain.user.repository.InkLogRepository;
import com.example.egobook_be.domain.user.repository.UserRepository;
import com.example.egobook_be.global.exception.CustomException;
import com.example.egobook_be.global.util.InkLogUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TodayQuestionCreateAnswerServiceTest {

    @InjectMocks
    private TodayQuestionService todayQuestionService;

    @Mock private TodayQuestionRepository todayQuestionRepository;
    @Mock private QuestionAnswerRepository questionAnswerRepository;
    @Mock private MissionRepository missionRepository;
    @Mock private UserRepository userRepository;
    @Mock private InkLogRepository inkLogRepository;
    @Mock private AbilityRepository abilityRepository;
    @Mock private InkLogUtil inkLogUtil;
    @Mock private com.example.egobook_be.domain.friend.repository.FriendRepository friendRepository;

    private User user;
    private TodayQuestion todayQuestion;
    private Mission mission;
    private Ability ability;
    private AnswerCreateReqDto reqDto;

    @BeforeEach
    void setUp() {
        user = mock(User.class);
        todayQuestion = mock(TodayQuestion.class);
        mission = mock(Mission.class);
        ability = mock(Ability.class);

        reqDto = new AnswerCreateReqDto("오늘의 답변입니다.", AnswerVisibility.PUBLIC);
    }

    @Test
    @DisplayName("createAnswer_정상답변저장_성공")
    void createAnswer_validRequest_success() {

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(missionRepository.findByUser(user)).willReturn(Optional.of(mission));
        given(abilityRepository.findByUser(user)).willReturn(Optional.of(ability));
        given(todayQuestionRepository.findByQuestionDate(LocalDate.now()))
                .willReturn(Optional.of(todayQuestion));
        given(questionAnswerRepository.existsByUserAndQuestion(user, todayQuestion)).willReturn(false);
        given(questionAnswerRepository.existsByUserAndCreatedAtBetween(
                eq(user), any(LocalDateTime.class), any(LocalDateTime.class)
        )).willReturn(false);
        given(mission.updateDailyQuestionMissionStatus(true)).willReturn(false);
        given(ability.addDiligence(1)).willReturn(0);

        todayQuestionService.createAnswer(1L, reqDto);

        ArgumentCaptor<QuestionAnswer> captor = ArgumentCaptor.forClass(QuestionAnswer.class);
        verify(questionAnswerRepository).save(captor.capture());
        assertThat(captor.getValue().getContent()).isEqualTo("오늘의 답변입니다.");
        assertThat(captor.getValue().getVisibility()).isEqualTo(AnswerVisibility.PUBLIC);
    }

    @Test
    @DisplayName("createAnswer_사용자없음_실패")
    void createAnswer_userNotFound_fail() {

        given(userRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> todayQuestionService.createAnswer(1L, reqDto))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(UserErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("createAnswer_미션정보없음_실패")
    void createAnswer_missionNotFound_fail() {

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(missionRepository.findByUser(user)).willReturn(Optional.empty());

        assertThatThrownBy(() -> todayQuestionService.createAnswer(1L, reqDto))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(UserErrorCode.MISSION_NOT_FOUND);
    }

    @Test
    @DisplayName("createAnswer_능력치정보없음_실패")
    void createAnswer_abilityNotFound_fail() {

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(missionRepository.findByUser(user)).willReturn(Optional.of(mission));
        given(abilityRepository.findByUser(user)).willReturn(Optional.empty());

        assertThatThrownBy(() -> todayQuestionService.createAnswer(1L, reqDto))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(UserErrorCode.ABILITY_NOT_FOUND);
    }

    @Test
    @DisplayName("createAnswer_오늘의질문없음_실패")
    void createAnswer_todayQuestionNotFound_fail() {

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(missionRepository.findByUser(user)).willReturn(Optional.of(mission));
        given(abilityRepository.findByUser(user)).willReturn(Optional.of(ability));
        given(todayQuestionRepository.findByQuestionDate(LocalDate.now()))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> todayQuestionService.createAnswer(1L, reqDto))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(QuestionErrorCode.TODAY_QUESTION_NOT_FOUND);
    }

    @Test
    @DisplayName("createAnswer_오늘이미답변함_실패")
    void createAnswer_alreadyAnsweredToday_fail() {

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(missionRepository.findByUser(user)).willReturn(Optional.of(mission));
        given(abilityRepository.findByUser(user)).willReturn(Optional.of(ability));
        given(todayQuestionRepository.findByQuestionDate(LocalDate.now()))
                .willReturn(Optional.of(todayQuestion));
        given(questionAnswerRepository.existsByUserAndQuestion(user, todayQuestion)).willReturn(true);

        assertThatThrownBy(() -> todayQuestionService.createAnswer(1L, reqDto))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(QuestionErrorCode.ALREADY_ANSWERED_TODAY);
    }

    @Test
    @DisplayName("createAnswer_첫답변시잉크보상지급_성공")
    void createAnswer_firstAnswerToday_inkRewardGiven() {

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(missionRepository.findByUser(user)).willReturn(Optional.of(mission));
        given(abilityRepository.findByUser(user)).willReturn(Optional.of(ability));
        given(todayQuestionRepository.findByQuestionDate(LocalDate.now()))
                .willReturn(Optional.of(todayQuestion));
        given(questionAnswerRepository.existsByUserAndQuestion(user, todayQuestion)).willReturn(false);
        given(questionAnswerRepository.existsByUserAndCreatedAtBetween(
                eq(user), any(LocalDateTime.class), any(LocalDateTime.class)
        )).willReturn(false);
        given(mission.updateDailyQuestionMissionStatus(true)).willReturn(false);
        given(ability.addDiligence(1)).willReturn(0);

        todayQuestionService.createAnswer(1L, reqDto);

        verify(inkLogUtil, atLeastOnce()).addInkLogToList(any(), eq(user), anyInt(), any());
    }

    @Test
    @DisplayName("createAnswer_일일미션최초달성시추가잉크지급_성공")
    void createAnswer_dailyMissionFirstComplete_bonusInkGiven() {

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(missionRepository.findByUser(user)).willReturn(Optional.of(mission));
        given(abilityRepository.findByUser(user)).willReturn(Optional.of(ability));
        given(todayQuestionRepository.findByQuestionDate(LocalDate.now()))
                .willReturn(Optional.of(todayQuestion));
        given(questionAnswerRepository.existsByUserAndQuestion(user, todayQuestion)).willReturn(false);
        given(questionAnswerRepository.existsByUserAndCreatedAtBetween(
                eq(user), any(LocalDateTime.class), any(LocalDateTime.class)
        )).willReturn(false);
        given(mission.updateDailyQuestionMissionStatus(true)).willReturn(true);
        given(mission.isWeeklyMissionCompleted()).willReturn(false);
        given(ability.addDiligence(1)).willReturn(0);

        todayQuestionService.createAnswer(1L, reqDto);

        verify(inkLogUtil, atLeast(2)).addInkLogToList(any(), eq(user), anyInt(), any());
    }
}