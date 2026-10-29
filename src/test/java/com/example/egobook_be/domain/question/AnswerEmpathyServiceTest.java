package com.example.egobook_be.domain.question;

import com.example.egobook_be.domain.question.dto.AnswerEmpathyResDto;
import com.example.egobook_be.domain.question.entity.AnswerEmpathy;
import com.example.egobook_be.domain.question.entity.QuestionAnswer;
import com.example.egobook_be.domain.question.enums.AnswerVisibility;
import com.example.egobook_be.domain.question.exception.QuestionErrorCode;
import com.example.egobook_be.domain.question.repository.AnswerEmpathyRepository;
import com.example.egobook_be.domain.question.repository.QuestionAnswerRepository;
import com.example.egobook_be.domain.question.service.AnswerEmpathyService;
import com.example.egobook_be.domain.user.entity.User;
import com.example.egobook_be.domain.user.repository.UserRepository;
import com.example.egobook_be.global.exception.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnswerEmpathyServiceTest {

    @InjectMocks
    private AnswerEmpathyService answerEmpathyService;

    @Mock private AnswerEmpathyRepository answerEmpathyRepository;
    @Mock private QuestionAnswerRepository questionAnswerRepository;
    @Mock private UserRepository userRepository;

    @Test
    @DisplayName("toggleEmpathy_공감하지 않은 공개 답변_공감 활성화")
    void toggleEmpathy_notEmpathized_activate() {
        QuestionAnswer answer = mock(QuestionAnswer.class);
        given(answer.getVisibility()).willReturn(AnswerVisibility.PUBLIC);
        given(questionAnswerRepository.findByIdForUpdate(10L)).willReturn(Optional.of(answer));
        given(answerEmpathyRepository.findByAnswer_IdAndUser_Id(10L, 1L)).willReturn(Optional.empty());
        given(userRepository.findById(1L)).willReturn(Optional.of(mock(User.class)));
        given(answerEmpathyRepository.countByAnswer_Id(10L)).willReturn(3L);

        AnswerEmpathyResDto result = answerEmpathyService.toggleEmpathy(1L, 10L);

        verify(answerEmpathyRepository).save(any(AnswerEmpathy.class));
        assertThat(result.answerId()).isEqualTo(10L);
        assertThat(result.empathized()).isTrue();
        assertThat(result.empathyCount()).isEqualTo(3L);
    }

    @Test
    @DisplayName("toggleEmpathy_이미 공감한 답변_공감 비활성화")
    void toggleEmpathy_alreadyEmpathized_deactivate() {
        QuestionAnswer answer = mock(QuestionAnswer.class);
        AnswerEmpathy empathy = mock(AnswerEmpathy.class);
        given(questionAnswerRepository.findByIdForUpdate(10L)).willReturn(Optional.of(answer));
        given(answerEmpathyRepository.findByAnswer_IdAndUser_Id(10L, 1L)).willReturn(Optional.of(empathy));
        given(answerEmpathyRepository.countByAnswer_Id(10L)).willReturn(2L);

        AnswerEmpathyResDto result = answerEmpathyService.toggleEmpathy(1L, 10L);

        verify(answerEmpathyRepository).delete(empathy);
        verify(answerEmpathyRepository, never()).save(any());
        assertThat(result.empathized()).isFalse();
        assertThat(result.empathyCount()).isEqualTo(2L);
    }

    @Test
    @DisplayName("toggleEmpathy_공개되지 않은 답변_실패")
    void toggleEmpathy_notPublicAnswer_fail() {
        QuestionAnswer answer = mock(QuestionAnswer.class);
        given(answer.getVisibility()).willReturn(AnswerVisibility.FRIEND);
        given(questionAnswerRepository.findByIdForUpdate(10L)).willReturn(Optional.of(answer));
        given(answerEmpathyRepository.findByAnswer_IdAndUser_Id(10L, 1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> answerEmpathyService.toggleEmpathy(1L, 10L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(QuestionErrorCode.EMPATHY_NOT_ALLOWED);

        verify(answerEmpathyRepository, never()).save(any());
    }

    @Test
    @DisplayName("toggleEmpathy_답변없음_실패")
    void toggleEmpathy_answerNotFound_fail() {
        given(questionAnswerRepository.findByIdForUpdate(10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> answerEmpathyService.toggleEmpathy(1L, 10L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(QuestionErrorCode.ANSWER_NOT_FOUND);
    }
}
