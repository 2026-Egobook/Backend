package com.example.egobook_be.domain.question;

import com.example.egobook_be.domain.friend.repository.FriendRepository;
import com.example.egobook_be.domain.home.repository.MissionRepository;
import com.example.egobook_be.domain.question.entity.QuestionAnswer;
import com.example.egobook_be.domain.question.exception.QuestionErrorCode;
import com.example.egobook_be.domain.question.repository.QuestionAnswerRepository;
import com.example.egobook_be.domain.question.repository.TodayQuestionRepository;
import com.example.egobook_be.domain.question.service.TodayQuestionService;
import com.example.egobook_be.domain.user.entity.User;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TodayQuestionDeleteAnswerServiceTest {

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

    private User user;

    @BeforeEach
    void setUp() {
        user = mock(User.class);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
    }

    @Test
    @DisplayName("deleteAnswer_정상삭제_성공")
    void deleteAnswer_validRequest_success() {

        QuestionAnswer answer = mock(QuestionAnswer.class);
        given(questionAnswerRepository.findByIdAndUser(10L, user))
                .willReturn(Optional.of(answer));

        todayQuestionService.deleteAnswer(1L, 10L);

        verify(questionAnswerRepository).delete(answer);
    }

    @Test
    @DisplayName("deleteAnswer_답변없음_실패")
    void deleteAnswer_answerNotFound_fail() {

        given(questionAnswerRepository.findByIdAndUser(10L, user))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> todayQuestionService.deleteAnswer(1L, 10L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(QuestionErrorCode.ANSWER_NOT_FOUND);
    }
}
