package com.example.egobook_be.domain.question;

import com.example.egobook_be.domain.friend.repository.FriendRepository;
import com.example.egobook_be.domain.home.repository.MissionRepository;
import com.example.egobook_be.domain.question.dto.TodayQuestionResDto;
import com.example.egobook_be.domain.question.entity.QuestionAnswer;
import com.example.egobook_be.domain.question.entity.TodayQuestion;
import com.example.egobook_be.domain.question.enums.AnswerVisibility;
import com.example.egobook_be.domain.question.exception.QuestionErrorCode;
import com.example.egobook_be.domain.question.repository.QuestionAnswerRepository;
import com.example.egobook_be.domain.question.repository.TodayQuestionRepository;
import com.example.egobook_be.domain.question.service.TodayQuestionService;
import com.example.egobook_be.domain.user.repository.AbilityRepository;
import com.example.egobook_be.domain.user.repository.InkLogRepository;
import com.example.egobook_be.domain.user.repository.UserRepository;
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
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class TodayQuestionAnsweredStatusServiceTest {

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

    private TodayQuestion todayQuestion;

    @BeforeEach
    void setUp() {
        todayQuestion = mock(TodayQuestion.class);
        given(todayQuestion.getId()).willReturn(100L);
        given(todayQuestion.getContent()).willReturn("오늘의 질문입니다.");
        given(todayQuestion.getQuestionDate()).willReturn(LocalDate.now());
        given(todayQuestionRepository.findByQuestionDate(LocalDate.now()))
                .willReturn(Optional.of(todayQuestion));
    }

    @Test
    @DisplayName("getTodayQuestion_오늘의질문없음_실패")
    void getTodayQuestion_questionNotFound_fail() {

        given(todayQuestionRepository.findByQuestionDate(LocalDate.now()))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> todayQuestionService.getTodayQuestion(1L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(QuestionErrorCode.TODAY_QUESTION_NOT_FOUND);
    }

    @Test
    @DisplayName("getTodayQuestion_답변안한경우_answered가false_성공")
    void getTodayQuestion_notAnsweredYet_answeredFalse() {

        given(questionAnswerRepository.findByUserIdAndQuestionIdWithQuestion(1L, 100L))
                .willReturn(Optional.empty());

        TodayQuestionResDto result = todayQuestionService.getTodayQuestion(1L);

        assertThat(result.answered()).isFalse();
        assertThat(result.myAnswer()).isNull();
    }

    @Test
    @DisplayName("getTodayQuestion_이미답변한경우_answered가true_성공")
    void getTodayQuestion_alreadyAnswered_answeredTrue() {

        QuestionAnswer answer = mock(QuestionAnswer.class);
        given(answer.getId()).willReturn(10L);
        given(answer.getContent()).willReturn("내 답변");
        given(answer.getVisibility()).willReturn(AnswerVisibility.PUBLIC);
        given(answer.getCreatedAt()).willReturn(LocalDateTime.now());

        given(questionAnswerRepository.findByUserIdAndQuestionIdWithQuestion(1L, 100L))
                .willReturn(Optional.of(answer));

        TodayQuestionResDto result = todayQuestionService.getTodayQuestion(1L);

        assertThat(result.answered()).isTrue();
        assertThat(result.myAnswer()).isNotNull();
        assertThat(result.myAnswer().content()).isEqualTo("내 답변");
        assertThat(result.myAnswer().visibility()).isEqualTo(AnswerVisibility.PUBLIC);
    }

    @Test
    @DisplayName("getTodayQuestion_비로그인유저_answered가false_성공")
    void getTodayQuestion_anonymousUser_answeredFalse() {

        TodayQuestionResDto result = todayQuestionService.getTodayQuestion(null);

        assertThat(result.answered()).isFalse();
        assertThat(result.myAnswer()).isNull();
    }
}