package com.example.egobook_be.domain.freind;

import com.example.egobook_be.domain.friend.entity.Friend;
import com.example.egobook_be.domain.friend.entity.FriendRequest;
import com.example.egobook_be.domain.friend.exception.FriendErrorCode;
import com.example.egobook_be.domain.friend.repository.FriendRepository;
import com.example.egobook_be.domain.friend.repository.FriendRequestRepository;
import com.example.egobook_be.domain.friend.service.FriendService;
import com.example.egobook_be.domain.user.entity.User;
import com.example.egobook_be.domain.user.repository.UserRepository;
import com.example.egobook_be.global.exception.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FriendAcceptServiceTest {

    @InjectMocks
    private FriendService friendService;

    @Mock
    private FriendRequestRepository friendRequestRepository;

    @Mock
    private FriendRepository friendRepository;

    @Mock
    private UserRepository userRepository;

    private User sender;
    private User receiver;
    private FriendRequest friendRequest;

    @BeforeEach
    void setUp() {
        sender = mock(User.class);
        receiver = mock(User.class);
        friendRequest = mock(FriendRequest.class);
    }

    @Test
    @DisplayName("acceptRequest_정상수락_성공")
    void acceptRequest_validRequest_success() {
        // given
        given(friendRequest.getSender()).willReturn(sender);
        given(userRepository.findById(2L)).willReturn(Optional.of(receiver));
        given(friendRequestRepository.findByIdAndReceiver(10L, receiver))
                .willReturn(Optional.of(friendRequest));
        given(friendRepository.existsByUserAndFriend(receiver, sender)).willReturn(false);
        given(friendRepository.countByUser(receiver)).willReturn(0L);
        given(friendRepository.countByUser(sender)).willReturn(0L);

        // when
        friendService.acceptRequest(2L, 10L);

        // then
        verify(friendRequest).accept();
        verify(friendRepository, times(2)).save(any(Friend.class));
    }

    @Test
    @DisplayName("acceptRequest_수신자없음_실패")
    void acceptRequest_receiverNotFound_fail() {
        // given
        given(userRepository.findById(2L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> friendService.acceptRequest(2L, 10L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(FriendErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("acceptRequest_신청내역없음_실패")
    void acceptRequest_requestNotFound_fail() {
        // given
        given(userRepository.findById(2L)).willReturn(Optional.of(receiver));
        given(friendRequestRepository.findByIdAndReceiver(10L, receiver))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> friendService.acceptRequest(2L, 10L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(FriendErrorCode.FRIEND_REQUEST_NOT_FOUND);
    }

    @Test
    @DisplayName("acceptRequest_이미친구인경우_실패")
    void acceptRequest_alreadyFriend_fail() {
        // given
        given(friendRequest.getSender()).willReturn(sender);
        given(userRepository.findById(2L)).willReturn(Optional.of(receiver));
        given(friendRequestRepository.findByIdAndReceiver(10L, receiver))
                .willReturn(Optional.of(friendRequest));
        given(friendRepository.existsByUserAndFriend(receiver, sender)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> friendService.acceptRequest(2L, 10L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(FriendErrorCode.ALREADY_FRIEND);
    }

    @Test
    @DisplayName("acceptRequest_수신자친구10명초과_실패")
    void acceptRequest_receiverFriendLimitExceeded_fail() {
        // given
        given(friendRequest.getSender()).willReturn(sender);
        given(userRepository.findById(2L)).willReturn(Optional.of(receiver));
        given(friendRequestRepository.findByIdAndReceiver(10L, receiver))
                .willReturn(Optional.of(friendRequest));
        given(friendRepository.existsByUserAndFriend(receiver, sender)).willReturn(false);
        given(friendRepository.countByUser(receiver)).willReturn(10L);

        // when & then
        assertThatThrownBy(() -> friendService.acceptRequest(2L, 10L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(FriendErrorCode.FRIEND_LIMIT_EXCEEDED);
    }

    @Test
    @DisplayName("acceptRequest_발신자친구10명초과_실패")
    void acceptRequest_senderFriendLimitExceeded_fail() {
        // given
        given(friendRequest.getSender()).willReturn(sender);
        given(userRepository.findById(2L)).willReturn(Optional.of(receiver));
        given(friendRequestRepository.findByIdAndReceiver(10L, receiver))
                .willReturn(Optional.of(friendRequest));
        given(friendRepository.existsByUserAndFriend(receiver, sender)).willReturn(false);
        given(friendRepository.countByUser(receiver)).willReturn(0L);
        given(friendRepository.countByUser(sender)).willReturn(10L);

        // when & then
        assertThatThrownBy(() -> friendService.acceptRequest(2L, 10L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(FriendErrorCode.FRIEND_LIMIT_EXCEEDED);
    }

    @Test
    @DisplayName("acceptRequest_동시요청낙관적락충돌_실패")
    void acceptRequest_optimisticLockConflict_fail() {
        // given
        given(friendRequest.getSender()).willReturn(sender);
        given(userRepository.findById(2L)).willReturn(Optional.of(receiver));
        given(friendRequestRepository.findByIdAndReceiver(10L, receiver))
                .willReturn(Optional.of(friendRequest));
        given(friendRepository.existsByUserAndFriend(receiver, sender)).willReturn(false);
        given(friendRepository.countByUser(receiver)).willReturn(0L);
        given(friendRepository.countByUser(sender)).willReturn(0L);
        doThrow(ObjectOptimisticLockingFailureException.class).when(friendRequest).accept();

        // when & then
        assertThatThrownBy(() -> friendService.acceptRequest(2L, 10L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(FriendErrorCode.FRIEND_REQUEST_CONFLICT);
    }
}

