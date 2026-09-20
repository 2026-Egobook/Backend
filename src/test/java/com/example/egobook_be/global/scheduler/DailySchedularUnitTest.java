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
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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
        User user = User.builder().id(1L).status(UserStatus.WITHDRAW_PENDING)
                .purgeAt(LocalDateTime.now().minusDays(1)).build();
        given(userRepository.findByStatusAndPurgeAtBefore(
                eq(UserStatus.WITHDRAW_PENDING), any(LocalDateTime.class)))
                .willReturn(List.of(user));
        given(userRepository.findByIdWithLock(1L)).willReturn(Optional.of(user));
        given(transactionTemplate.execute(any())).willAnswer(invocation -> {
            TransactionCallback<Object> callback = invocation.getArgument(0);
            return callback.doInTransaction(mock(TransactionStatus.class));
        });

        // when
        dailySchedular.purgeUsers();

        // then
        List<User> users = List.of(user);
        verify(transactionTemplate, times(1)).execute(any());
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

    @Test
    @DisplayName("조회 이후 복구되어 ACTIVE가 된 사용자는 삭제하지 않는다")
    void 조회_이후_복구된_사용자는_삭제하지_않는다() {
        // given
        User target = User.builder().id(1L).status(UserStatus.WITHDRAW_PENDING)
                .purgeAt(LocalDateTime.now().minusDays(1)).build();
        User restored = User.builder().id(1L).status(UserStatus.ACTIVE).build();
        given(userRepository.findByStatusAndPurgeAtBefore(
                eq(UserStatus.WITHDRAW_PENDING), any(LocalDateTime.class)))
                .willReturn(List.of(target));
        given(userRepository.findByIdWithLock(1L)).willReturn(Optional.of(restored));
        given(transactionTemplate.execute(any())).willAnswer(invocation -> {
            TransactionCallback<Object> callback = invocation.getArgument(0);
            return callback.doInTransaction(mock(TransactionStatus.class));
        });

        // when
        dailySchedular.purgeUsers();

        // then
        verify(userRepository, never()).deleteAll(any());
        verify(adRewardHistoryRepository, never()).bulkDeleteByUserIn(any());
    }
}
