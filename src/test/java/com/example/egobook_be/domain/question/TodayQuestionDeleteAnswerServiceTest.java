package com.example.egobook_be.domain.question;

import com.example.egobook_be.domain.friend.repository.FriendRepository;
import com.example.egobook_be.domain.home.repository.MissionRepository;
import com.example.egobook_be.domain.question.entity.QuestionAnswer;
import com.example.egobook_be.domain.question.exception.QuestionErrorCode;
import com.example.egobook_be.domain.moderation.entity.ModerationReportArchive;
import com.example.egobook_be.domain.moderation.repository.ModerationReportArchiveRepository;
import com.example.egobook_be.domain.question.entity.AnswerReport;
import com.example.egobook_be.domain.question.repository.AnswerEmpathyRepository;
import com.example.egobook_be.domain.question.repository.AnswerReportRepository;
import com.example.egobook_be.global.enums.ReportReason;
import com.example.egobook_be.global.enums.ReportStatus;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TodayQuestionDeleteAnswerServiceTest {

    @InjectMocks
    private TodayQuestionService todayQuestionService;

    @Mock private TodayQuestionRepository todayQuestionRepository;
    @Mock private QuestionAnswerRepository questionAnswerRepository;
    @Mock private AnswerEmpathyRepository answerEmpathyRepository;
    @Mock private AnswerReportRepository answerReportRepository;
    @Mock private ModerationReportArchiveRepository archiveRepository;
    @Mock private MissionRepository missionRepository;
    @Mock private UserRepository userRepository;
    @Mock private InkLogRepository inkLogRepository;
    @Mock private AbilityRepository abilityRepository;
    @Mock private FriendRepository friendRepository;
    @Mock private InkLogUtil inkLogUtil;
    @Mock private RestrictionGuardService restrictionGuardService;

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

        verify(answerReportRepository, never()).deleteAllByAnswerId(any());
        verify(answerEmpathyRepository).deleteAllByAnswerId(10L);
        verify(questionAnswerRepository).delete(answer);
    }

    @Test
    @DisplayName("deleteAnswer_신고된 답변_신고 보관 후 삭제 성공")
    void deleteAnswer_reportedAnswer_archiveAndDelete() {
        User reporter = mock(User.class);
        given(reporter.getId()).willReturn(2L);
        given(user.getId()).willReturn(1L);

        QuestionAnswer answer = mock(QuestionAnswer.class);
        given(answer.getUser()).willReturn(user);
        given(answer.getContent()).willReturn("답변 내용");
        given(questionAnswerRepository.findByIdAndUser(10L, user))
                .willReturn(Optional.of(answer));

        AnswerReport report = mock(AnswerReport.class);
        given(report.getId()).willReturn(100L);
        given(report.getUser()).willReturn(reporter);
        given(report.getReason()).willReturn(ReportReason.values()[0]);
        given(report.getStatus()).willReturn(ReportStatus.PENDING);
        given(answerReportRepository.findAllByAnswerId(10L)).willReturn(List.of(report));

        todayQuestionService.deleteAnswer(1L, 10L);

        var inOrder = inOrder(archiveRepository, answerReportRepository, answerEmpathyRepository, questionAnswerRepository);
        inOrder.verify(archiveRepository).saveAll(argThat((List<ModerationReportArchive> list) ->
                list.size() == 1
                        && list.get(0).getSourceReportId().equals(100L)
                        && list.get(0).getOriginalContent().equals("답변 내용")));
        inOrder.verify(answerReportRepository).deleteAllByAnswerId(10L);
        inOrder.verify(answerEmpathyRepository).deleteAllByAnswerId(10L);
        inOrder.verify(questionAnswerRepository).delete(answer);
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

    // [AI-GEN] RestrictionGuardService 적용 이후 추가된 제재 관련 테스트 케이스

    @Test
    @DisplayName("deleteAnswer_QUESTION_ANSWER 제재 중_예외")
    void deleteAnswer_questionAnswerRestricted_fail() {
        // given
        QuestionAnswer answer = mock(QuestionAnswer.class);
        given(questionAnswerRepository.findByIdAndUser(10L, user))
                .willReturn(Optional.of(answer));
        willThrow(new CustomException(RestrictionErrorCode.QUESTION_ANSWER_RESTRICTED))
                .given(restrictionGuardService).checkQuestionAnswerRestriction(1L);

        // when & then
        assertThatThrownBy(() -> todayQuestionService.deleteAnswer(1L, 10L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(RestrictionErrorCode.QUESTION_ANSWER_RESTRICTED);

        verify(answerEmpathyRepository, never()).deleteAllByAnswerId(any());
        verify(questionAnswerRepository, never()).delete(any());
    }
}
