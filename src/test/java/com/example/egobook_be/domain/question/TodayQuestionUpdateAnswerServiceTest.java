package com.example.egobook_be.domain.question;

import com.example.egobook_be.domain.friend.repository.FriendRepository;
import com.example.egobook_be.domain.home.repository.MissionRepository;
import com.example.egobook_be.domain.question.dto.AnswerUpdateReqDto;
import com.example.egobook_be.domain.question.entity.QuestionAnswer;
import com.example.egobook_be.domain.question.entity.TodayQuestion;
import com.example.egobook_be.domain.question.enums.AnswerVisibility;
import com.example.egobook_be.domain.question.exception.QuestionErrorCode;
import com.example.egobook_be.domain.question.repository.QuestionAnswerRepository;
import com.example.egobook_be.domain.question.repository.TodayQuestionRepository;
import com.example.egobook_be.domain.question.service.TodayQuestionService;
import com.example.egobook_be.domain.user.entity.User;
import com.example.egobook_be.domain.user.repository.AbilityRepository;
import com.example.egobook_be.domain.user.repository.InkLogRepository;
import com.example.egobook_be.domain.user.repository.UserRepository;
import com.example.egobook_be.domain.restriction.exception.RestrictionErrorCode;
import com.example.egobook_be.domain.restriction.service.RestrictionGuardService;
import com.example.egobook_be.global.exception.CustomException;
import com.example.egobook_be.global.util.InkLogUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TodayQuestionUpdateAnswerServiceTest {

    @InjectMocks
    private TodayQuestionService todayQuestionService;

    @Mock private TodayQuestionRepository todayQuestionRepository;
    @Mock private QuestionAnswerRepository questionAnswerRepository;
    @Mock private MissionRepository missionRepository;
    @Mock private UserRepository userRepository;
    @Mock private InkLogRepository inkLogRepository;
    @Mock private AbilityRepository abilityRepository;
    @Mock private FriendRepository friendRepository;
    @Mock private InkLogUtil inkLogUtil;
    @Mock private RestrictionGuardService restrictionGuardService;

    private User user;
    private TodayQuestion todayQuestion;
    private AnswerUpdateReqDto reqDto;

    @BeforeEach
    void setUp() {
        user = mock(User.class);
        todayQuestion = mock(TodayQuestion.class);

        reqDto = AnswerUpdateReqDto.builder()
                .content("수정된 답변입니다.")
                .visibility(AnswerVisibility.FRIEND)
                .build();

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
    }

    @Test
    @DisplayName("updateAnswer_정상수정_성공")
    void updateAnswer_validRequest_success() {

        QuestionAnswer answer = mock(QuestionAnswer.class);
        given(todayQuestionRepository.findByQuestionDate(LocalDate.now()))
                .willReturn(Optional.of(todayQuestion));
        given(questionAnswerRepository.findByUserAndQuestion(user, todayQuestion))
                .willReturn(Optional.of(answer));

        todayQuestionService.updateAnswer(1L, reqDto);

        verify(answer).update("수정된 답변입니다.", AnswerVisibility.FRIEND);
    }

    @Test
    @DisplayName("updateAnswer_오늘의질문없음_실패")
    void updateAnswer_todayQuestionNotFound_fail() {

        given(todayQuestionRepository.findByQuestionDate(LocalDate.now()))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> todayQuestionService.updateAnswer(1L, reqDto))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(QuestionErrorCode.TODAY_QUESTION_NOT_FOUND);
    }

    @Test
    @DisplayName("updateAnswer_답변없음_실패")
    void updateAnswer_answerNotFound_fail() {

        given(todayQuestionRepository.findByQuestionDate(LocalDate.now()))
                .willReturn(Optional.of(todayQuestion));
        given(questionAnswerRepository.findByUserAndQuestion(user, todayQuestion))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> todayQuestionService.updateAnswer(1L, reqDto))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(QuestionErrorCode.ANSWER_NOT_FOUND);
    }

    // [AI-GEN] RestrictionGuardService 적용 이후 추가된 제재 관련 테스트 케이스

    @Test
    @DisplayName("updateAnswer_QUESTION_ANSWER 제재 중_예외")
    void updateAnswer_questionAnswerRestricted_fail() {
        // given
        QuestionAnswer answer = mock(QuestionAnswer.class);
        given(todayQuestionRepository.findByQuestionDate(LocalDate.now()))
                .willReturn(Optional.of(todayQuestion));
        given(questionAnswerRepository.findByUserAndQuestion(user, todayQuestion))
                .willReturn(Optional.of(answer));
        willThrow(new CustomException(RestrictionErrorCode.QUESTION_ANSWER_RESTRICTED))
                .given(restrictionGuardService).checkQuestionAnswerRestriction(1L);

        // when & then
        assertThatThrownBy(() -> todayQuestionService.updateAnswer(1L, reqDto))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(RestrictionErrorCode.QUESTION_ANSWER_RESTRICTED);

        verify(answer, never()).update(any(), any());
    }
}