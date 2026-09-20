package com.example.egobook_be.global.scheduler;

import com.example.egobook_be.domain.ads.repository.AdRewardHistoryRepository;
import com.example.egobook_be.domain.auth.repository.AuthAccountRepository;
import com.example.egobook_be.domain.auth.repository.RefreshTokenBackupRepository;
import com.example.egobook_be.domain.coupon.repository.UserCouponRepository;
import com.example.egobook_be.domain.diary.repository.DiaryRepository;
import com.example.egobook_be.domain.ego_room.repository.DailyPraiseRepository;
import com.example.egobook_be.domain.ego_room.repository.UserStatsRepository;
import com.example.egobook_be.domain.ego_room.repository.WeeklyCounselRepository;
import com.example.egobook_be.domain.friend.repository.FriendRepository;
import com.example.egobook_be.domain.friend.repository.FriendRequestRepository;
import com.example.egobook_be.domain.home.repository.MissionRepository;
import com.example.egobook_be.domain.letters.repository.PlazaLetterReplyReportRepository;
import com.example.egobook_be.domain.letters.repository.PlazaLetterReplyRepository;
import com.example.egobook_be.domain.letters.repository.PlazaLetterReportRepository;
import com.example.egobook_be.domain.letters.repository.PlazaLetterRepository;
import com.example.egobook_be.domain.letters.repository.PlazaLetterThreadRepository;
import com.example.egobook_be.domain.notice.repository.NoticeReadRepository;
import com.example.egobook_be.domain.notification.repository.NotificationRepository;
import com.example.egobook_be.domain.psychology.repository.UserKnowledgeRepository;
import com.example.egobook_be.domain.question.repository.AnswerReportRepository;
import com.example.egobook_be.domain.question.repository.QuestionAnswerRepository;
import com.example.egobook_be.domain.restriction.repository.RestrictionRepository;
import com.example.egobook_be.domain.terms.repository.UserTermRepository;
import com.example.egobook_be.domain.user.entity.User;
import com.example.egobook_be.domain.user.enums.UserStatus;
import com.example.egobook_be.domain.user.repository.AbilityLogRepository;
import com.example.egobook_be.domain.user.repository.InkLogRepository;
import com.example.egobook_be.domain.user.repository.SubscriptionRepository;
import com.example.egobook_be.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DailySchedularUnitTest {

    @InjectMocks
    private DailySchedular dailySchedular;

    @Mock private RefreshTokenBackupRepository refreshTokenBackupRepository;
    @Mock private MissionRepository missionRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuthAccountRepository authAccountRepository;
    @Mock private UserTermRepository userTermRepository;
    @Mock private DiaryRepository diaryRepository;
    @Mock private FriendRepository friendRepository;
    @Mock private FriendRequestRepository friendRequestRepository;
    @Mock private PlazaLetterReplyReportRepository plazaLetterReplyReportRepository;
    @Mock private PlazaLetterReplyRepository plazaLetterReplyRepository;
    @Mock private NotificationRepository notificationRepository;
    @Mock private PlazaLetterRepository plazaLetterRepository;
    @Mock private UserKnowledgeRepository userKnowledgeRepository;
    @Mock private QuestionAnswerRepository questionAnswerRepository;
    @Mock private InkLogRepository inkLogRepository;
    @Mock private PlazaLetterThreadRepository plazaLetterThreadRepository;
    @Mock private RestrictionRepository restrictionRepository;
    @Mock private AdRewardHistoryRepository adRewardHistoryRepository;
    @Mock private UserCouponRepository userCouponRepository;
    @Mock private DailyPraiseRepository dailyPraiseRepository;
    @Mock private UserStatsRepository userStatsRepository;
    @Mock private WeeklyCounselRepository weeklyCounselRepository;
    @Mock private NoticeReadRepository noticeReadRepository;
    @Mock private AnswerReportRepository answerReportRepository;
    @Mock private AbilityLogRepository abilityLogRepository;
    @Mock private SubscriptionRepository subscriptionRepository;
    @Mock private PlazaLetterReportRepository plazaLetterReportRepository;
    @Mock private TransactionTemplate transactionTemplate;

    @Test
    @DisplayName("삭제 예정 사용자의 누락 연관 데이터 삭제 성공")
    void 삭제_예정_사용자의_누락_연관_데이터_삭제_성공() {
        // given
        User user = User.builder().id(1L).status(UserStatus.WITHDRAW_PENDING).build();
        given(userRepository.findByStatusAndPurgeAtBefore(
                eq(UserStatus.WITHDRAW_PENDING), any(LocalDateTime.class)))
                .willReturn(List.of(user));
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        doAnswer(invocation -> {
            Consumer<TransactionStatus> callback = invocation.getArgument(0);
            callback.accept(mock(TransactionStatus.class));
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());

        // when
        dailySchedular.purgeUsers();

        // then
        List<User> users = List.of(user);
        verify(transactionTemplate, times(1)).executeWithoutResult(any());
        verify(adRewardHistoryRepository).bulkDeleteByUserIn(users);
        verify(userCouponRepository).bulkDeleteByUserIn(users);
        verify(dailyPraiseRepository).bulkDeleteByUserIn(users);
        verify(userStatsRepository).bulkDeleteByUserIn(users);
        verify(weeklyCounselRepository).bulkDeleteByUserIn(users);
        verify(noticeReadRepository).bulkDeleteByUserIn(users);
        verify(answerReportRepository).bulkDeleteByUserOrAnswerUserIn(users);
        verify(abilityLogRepository).bulkDeleteByUserIn(users);
        verify(subscriptionRepository).bulkDeleteByUserIn(users);
        verify(userRepository).deleteAll(users);
    }
}
