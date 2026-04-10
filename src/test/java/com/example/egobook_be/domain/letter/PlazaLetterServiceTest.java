package com.example.egobook_be.domain.letter;

import com.example.egobook_be.domain.friend.repository.FriendRepository;
import com.example.egobook_be.domain.home.entity.Mission;
import com.example.egobook_be.domain.home.repository.MissionRepository;
import com.example.egobook_be.domain.letters.dto.request.CreateLetterRequest;
import com.example.egobook_be.domain.letters.dto.response.CreateLetterResponse;
import com.example.egobook_be.domain.letters.entity.PlazaLetter;
import com.example.egobook_be.domain.letters.entity.PlazaLetterMode;
import com.example.egobook_be.domain.letters.entity.PlazaLetterStatus;
import com.example.egobook_be.domain.letters.entity.PlazaLetterThread;
import com.example.egobook_be.domain.letters.enums.LettersErrorCode;
import com.example.egobook_be.domain.letters.enums.PlazaLetterColor;
import com.example.egobook_be.domain.letters.mapper.PlazaLetterMapper;
import com.example.egobook_be.domain.letters.repository.PlazaLetterReplyRepository;
import com.example.egobook_be.domain.letters.repository.PlazaLetterRepository;
import com.example.egobook_be.domain.letters.repository.PlazaLetterThreadRepository;
import com.example.egobook_be.domain.letters.service.PlazaLetterService;
import com.example.egobook_be.domain.letters.service.WordClientService;
import com.example.egobook_be.domain.notification.service.NotificationService;
import com.example.egobook_be.domain.user.entity.Ability;
import com.example.egobook_be.domain.user.entity.User;
import com.example.egobook_be.domain.user.repository.AbilityRepository;
import com.example.egobook_be.domain.user.repository.InkLogRepository;
import com.example.egobook_be.domain.user.repository.UserRepository;
import com.example.egobook_be.domain.restriction.enums.RestrictionDomainType;
import com.example.egobook_be.domain.restriction.exception.RestrictionErrorCode;
import com.example.egobook_be.domain.restriction.service.RestrictionGuardService;
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
import org.springframework.test.util.ReflectionTestUtils;

import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlazaLetterServiceTest {

    @Mock
    private PlazaLetterRepository plazaLetterRepository;

    @Mock
    private PlazaLetterReplyRepository plazaLetterReplyRepository;

    @Mock
    private PlazaLetterThreadRepository plazaLetterThreadRepository;

    @Mock
    private FriendRepository friendRepository;

    @Mock
    private InkLogRepository inkLogRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MissionRepository missionRepository;

    @Mock
    private AbilityRepository abilityRepository;

    @Mock
    private WordClientService wordClient;

    @Mock
    private InkLogUtil inkLogUtil;

    @Mock
    private PlazaLetterMapper plazaLetterMapper;

    @Mock
    private NotificationService notificationService;

    @Mock
    private RestrictionGuardService restrictionGuardService;

    @InjectMocks
    private PlazaLetterService plazaLetterService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(plazaLetterService, "cloudfrontDomain", "https://cdn.test.com");
    }

    @Test
    @DisplayName("createLetter_RANDOM 후보 없음_WAITING 저장")
    void createLetter_RANDOM후보없음_WAITING저장() {

        Long userId = 1L;

        User sender = User.builder()
                .id(userId)
                .accountCode("ACC001")
                .nickname("효진")
                .ink(0)
                .build();

        Mission mission = Mission.builder()
                .user(sender)
                .build();

        CreateLetterRequest request = new CreateLetterRequest();
        ReflectionTestUtils.setField(request, "mode", PlazaLetterMode.RANDOM);
        ReflectionTestUtils.setField(request, "text", "익명으로 보내는 광장 편지");
        ReflectionTestUtils.setField(request, "backgroundColor", PlazaLetterColor.WHITE);

        PlazaLetterThread savedThread = PlazaLetterThread.builder()
                .threadId(10L)
                .createdAt(LocalDateTime.now())
                .build();

        given(userRepository.findById(userId)).willReturn(Optional.of(sender));
        given(missionRepository.findByUser(sender)).willReturn(Optional.of(mission));
        given(plazaLetterRepository.existsBySenderIdAndCreatedAtBetween(eq(userId), any(), any()))
                .willReturn(false);
        given(wordClient.detectAsync(anyString())).willReturn(Mono.empty());

        given(userRepository.findHighReplyRateCandidates(userId, 50))
                .willReturn(Collections.emptyList());

        given(plazaLetterThreadRepository.save(any(PlazaLetterThread.class)))
                .willReturn(savedThread);

        given(plazaLetterRepository.save(any(PlazaLetter.class)))
                .willAnswer(invocation -> {
                    PlazaLetter letter = invocation.getArgument(0);
                    return PlazaLetter.builder()
                            .letterId(100L)
                            .threadId(letter.getThreadId())
                            .senderId(letter.getSenderId())
                            .receiverId(letter.getReceiverId())
                            .mode(letter.getMode())
                            .fromLabel(letter.getFromLabel())
                            .content(letter.getContent())
                            .backgroundColor(letter.getBackgroundColor())
                            .status(letter.getStatus())
                            .createdAt(letter.getCreatedAt())
                            .arrivedAt(letter.getArrivedAt())
                            .replyDeadlineAt(letter.getReplyDeadlineAt())
                            .build();
                });


        CreateLetterResponse response = plazaLetterService.createLetter(userId, request);


        ArgumentCaptor<PlazaLetter> captor = ArgumentCaptor.forClass(PlazaLetter.class);
        verify(plazaLetterRepository).save(captor.capture());

        PlazaLetter savedLetter = captor.getValue();
        // createLetter는 AI 검수 전 ANALYZING 상태로 저장하며, 수신자 없으면 WAITING은 비동기로 처리됨
        assertThat(savedLetter.getStatus()).isEqualTo(PlazaLetterStatus.ANALYZING);
        assertThat(savedLetter.getReceiverId()).isNull();
        assertThat(savedLetter.getArrivedAt()).isNull();
        assertThat(savedLetter.getReplyDeadlineAt()).isNull();
        assertThat(savedLetter.getMode()).isEqualTo(PlazaLetterMode.RANDOM);
        assertThat(savedLetter.getBackgroundColor()).isEqualTo(PlazaLetterColor.WHITE);

        assertThat(response.letterId()).isEqualTo(100L);
        assertThat(response.threadId()).isEqualTo(10L);
        assertThat(response.status()).isEqualTo(PlazaLetterStatus.ANALYZING);
        assertThat(response.mode()).isEqualTo(PlazaLetterMode.RANDOM);
    }

    @Test
    @DisplayName("createLetter_FRIEND인데 친구 아님_예외")
    void createLetter_FRIEND인데친구아님_예외() {
        // given
        Long userId = 1L;
        Long friendId = 2L;

        User sender = User.builder()
                .id(userId)
                .accountCode("ACC001")
                .nickname("효진")
                .ink(0)
                .build();

        User receiver = User.builder()
                .id(friendId)
                .accountCode("ACC002")
                .nickname("민지")
                .ink(0)
                .build();

        Mission mission = Mission.builder()
                .user(sender)
                .build();

        CreateLetterRequest request = new CreateLetterRequest();
        ReflectionTestUtils.setField(request, "mode", PlazaLetterMode.FRIEND);
        ReflectionTestUtils.setField(request, "toFriendId", friendId);
        ReflectionTestUtils.setField(request, "text", "친구에게 보내는 편지");
        ReflectionTestUtils.setField(request, "backgroundColor", PlazaLetterColor.WHITE);

        given(userRepository.findById(userId)).willReturn(Optional.of(sender));
        given(missionRepository.findByUser(sender)).willReturn(Optional.of(mission));
        given(plazaLetterRepository.existsBySenderIdAndCreatedAtBetween(eq(userId), any(), any()))
                .willReturn(false);
        given(userRepository.findById(friendId)).willReturn(Optional.of(receiver));
        given(friendRepository.existsByUserAndFriend(sender, receiver)).willReturn(false);

        // when
        Throwable thrown = catchThrowable(() -> plazaLetterService.createLetter(userId, request));

        // then
        assertThat(thrown).isInstanceOf(CustomException.class);
        assertThat(((CustomException) thrown).getErrorCode()).isEqualTo(LettersErrorCode.NOT_FRIEND);

        verify(plazaLetterThreadRepository, never()).save(any());
        verify(plazaLetterRepository, never()).save(any());
    }

    @Test
    @DisplayName("replyToLetter_이미 답장 존재_예외")
    void replyToLetter_이미답장존재_예외() {
        // given
        Long userId = 2L;
        Long letterId = 101L;

        User receiver = User.builder()
                .id(userId)
                .accountCode("ACC002")
                .nickname("민지")
                .ink(0)
                .build();

        Ability ability = Ability.builder()
                .user(receiver)
                .build();

        PlazaLetter letter = PlazaLetter.builder()
                .letterId(letterId)
                .threadId(10L)
                .senderId(1L)
                .receiverId(userId)
                .mode(PlazaLetterMode.RANDOM)
                .fromLabel("익명")
                .content("원본 편지")
                .backgroundColor(PlazaLetterColor.WHITE)
                .status(PlazaLetterStatus.ARRIVED)
                .createdAt(LocalDateTime.now().minusHours(1))
                .arrivedAt(LocalDateTime.now().minusMinutes(30))
                .replyDeadlineAt(LocalDateTime.now().plusHours(23))
                .build();

        given(userRepository.findById(userId)).willReturn(Optional.of(receiver));
        given(abilityRepository.findByUser(receiver)).willReturn(Optional.of(ability));
        given(plazaLetterRepository.findById(letterId)).willReturn(Optional.of(letter));
        given(plazaLetterReplyRepository.existsByLetter(letter)).willReturn(true); // validateReplyable 이전에 존재 확인

        // when
        Throwable thrown = catchThrowable(() ->
                plazaLetterService.replyToLetter(userId, letterId, "답장 내용")
        );

        // then
        assertThat(thrown).isInstanceOf(CustomException.class);
        assertThat(((CustomException) thrown).getErrorCode()).isEqualTo(LettersErrorCode.ALREADY_REPLIED);

        verify(plazaLetterReplyRepository, never()).save(any());
        verify(notificationService, never()).createNotification(anyLong(), any(), anyLong(), any());
    }

    @Test
    @DisplayName("deleteThread_내 스레드 아님_예외")
    void deleteThread_내스레드아님_예외() {
        // given
        Long userId = 99L;
        Long threadId = 300L;

        PlazaLetter letter = PlazaLetter.builder()
                .letterId(1L)
                .threadId(threadId)
                .senderId(1L)
                .receiverId(2L)
                .mode(PlazaLetterMode.RANDOM)
                .fromLabel("익명")
                .content("편지 내용")
                .backgroundColor(PlazaLetterColor.WHITE)
                .status(PlazaLetterStatus.REPLIED)
                .createdAt(LocalDateTime.now())
                .build();

        given(plazaLetterThreadRepository.existsById(threadId)).willReturn(true);
        given(plazaLetterRepository.findByThreadId(threadId)).willReturn(Optional.of(letter));

        // when
        Throwable thrown = catchThrowable(() -> plazaLetterService.deleteThread(userId, threadId));

        // then
        assertThat(thrown).isInstanceOf(CustomException.class);
        assertThat(((CustomException) thrown).getErrorCode()).isEqualTo(LettersErrorCode.FORBIDDEN);

        verify(plazaLetterReplyRepository, never()).deleteByThreadId(anyLong());
        verify(plazaLetterRepository, never()).deleteByThreadId(anyLong());
        verify(plazaLetterThreadRepository, never()).deleteById(anyLong());
    }

    // [AI-GEN] RestrictionGuardService 적용 이후 추가된 제재 관련 테스트 케이스

    @Test
    @DisplayName("getNextArrivedLetter_LETTER 제재 중_예외")
    void getNextArrivedLetter_LETTER제재중_예외() {
        // given
        Long userId = 1L;
        willThrow(new CustomException(RestrictionErrorCode.LETTER_RESTRICTED))
                .given(restrictionGuardService).checkLetterRestriction(userId);

        // when & then
        assertThatThrownBy(() -> plazaLetterService.getNextArrivedLetter(userId))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(RestrictionErrorCode.LETTER_RESTRICTED);

        verify(plazaLetterRepository, never()).findInboxLetterForReply(anyLong(), anyLong());
    }

    @Test
    @DisplayName("createLetter_LETTER 제재 중_예외")
    void createLetter_LETTER제재중_예외() {
        // given
        Long userId = 1L;
        User sender = User.builder().id(userId).accountCode("ACC001").nickname("효진").ink(0).build();
        Mission mission = Mission.builder().user(sender).build();

        CreateLetterRequest request = new CreateLetterRequest();
        ReflectionTestUtils.setField(request, "mode", PlazaLetterMode.RANDOM);
        ReflectionTestUtils.setField(request, "text", "제재 중인 사용자가 보내는 편지");
        ReflectionTestUtils.setField(request, "backgroundColor", PlazaLetterColor.WHITE);

        given(userRepository.findById(userId)).willReturn(Optional.of(sender));
        given(missionRepository.findByUser(sender)).willReturn(Optional.of(mission));
        willThrow(new CustomException(RestrictionErrorCode.LETTER_RESTRICTED))
                .given(restrictionGuardService).checkLetterRestriction(userId);

        // when & then
        assertThatThrownBy(() -> plazaLetterService.createLetter(userId, request))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(RestrictionErrorCode.LETTER_RESTRICTED);

        verify(plazaLetterRepository, never()).save(any());
    }

    @Test
    @DisplayName("createLetter_RANDOM 후보가 모두 제재 중_WAITING 저장")
    void createLetter_RANDOM_모든후보가제재중_WAITING저장() {
        // given
        Long userId = 1L;
        Long restrictedCandidateId = 10L;

        User sender = User.builder().id(userId).accountCode("ACC001").nickname("효진").ink(0).build();
        Mission mission = Mission.builder().user(sender).build();

        CreateLetterRequest request = new CreateLetterRequest();
        ReflectionTestUtils.setField(request, "mode", PlazaLetterMode.RANDOM);
        ReflectionTestUtils.setField(request, "text", "랜덤 편지");
        ReflectionTestUtils.setField(request, "backgroundColor", PlazaLetterColor.WHITE);

        PlazaLetterThread savedThread = PlazaLetterThread.builder()
                .threadId(10L).createdAt(LocalDateTime.now()).build();

        given(userRepository.findById(userId)).willReturn(Optional.of(sender));
        given(missionRepository.findByUser(sender)).willReturn(Optional.of(mission));
        given(plazaLetterRepository.existsBySenderIdAndCreatedAtBetween(eq(userId), any(), any())).willReturn(false);
        given(wordClient.detectAsync(anyString())).willReturn(Mono.empty());
        given(userRepository.findHighReplyRateCandidates(userId, 50)).willReturn(List.of(restrictedCandidateId));
        given(restrictionGuardService.getActivelyRestrictedUserIds(RestrictionDomainType.LETTER))
                .willReturn(new HashSet<>(Set.of(restrictedCandidateId)));
        given(plazaLetterThreadRepository.save(any())).willReturn(savedThread);
        given(plazaLetterRepository.save(any(PlazaLetter.class))).willAnswer(inv -> {
            PlazaLetter l = inv.getArgument(0);
            return PlazaLetter.builder()
                    .letterId(100L).threadId(l.getThreadId()).senderId(l.getSenderId())
                    .receiverId(l.getReceiverId()).mode(l.getMode()).fromLabel(l.getFromLabel())
                    .content(l.getContent()).backgroundColor(l.getBackgroundColor())
                    .status(l.getStatus()).createdAt(l.getCreatedAt())
                    .arrivedAt(l.getArrivedAt()).replyDeadlineAt(l.getReplyDeadlineAt()).build();
        });

        // when
        plazaLetterService.createLetter(userId, request);

        // then — 모든 후보가 제재 중이므로 receiverId 미배정, ANALYZING 상태로 저장
        ArgumentCaptor<PlazaLetter> captor = ArgumentCaptor.forClass(PlazaLetter.class);
        verify(plazaLetterRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(PlazaLetterStatus.ANALYZING);
        assertThat(captor.getValue().getReceiverId()).isNull();
    }

    @Test
    @DisplayName("replyToLetter_LETTER 제재 중_예외")
    void replyToLetter_LETTER제재중_예외() {
        // given
        Long userId = 2L;
        Long letterId = 101L;

        User receiver = User.builder().id(userId).accountCode("ACC002").nickname("민지").ink(0).build();
        Ability ability = Ability.builder().user(receiver).build();

        PlazaLetter letter = PlazaLetter.builder()
                .letterId(letterId).threadId(10L).senderId(1L).receiverId(userId)
                .mode(PlazaLetterMode.RANDOM).fromLabel("익명").content("원본 편지")
                .backgroundColor(PlazaLetterColor.WHITE).status(PlazaLetterStatus.ARRIVED)
                .createdAt(LocalDateTime.now().minusHours(1))
                .arrivedAt(LocalDateTime.now().minusMinutes(30))
                .replyDeadlineAt(LocalDateTime.now().plusHours(23)).build();

        given(userRepository.findById(userId)).willReturn(Optional.of(receiver));
        given(abilityRepository.findByUser(receiver)).willReturn(Optional.of(ability));
        given(plazaLetterRepository.findById(letterId)).willReturn(Optional.of(letter));
        // existsByLetter는 guard 이후에 체크되므로 stub 불필요
        willThrow(new CustomException(RestrictionErrorCode.LETTER_RESTRICTED))
                .given(restrictionGuardService).checkLetterRestriction(userId);

        // when & then
        assertThatThrownBy(() -> plazaLetterService.replyToLetter(userId, letterId, "답장 내용"))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(RestrictionErrorCode.LETTER_RESTRICTED);

        verify(plazaLetterReplyRepository, never()).save(any());
    }
}