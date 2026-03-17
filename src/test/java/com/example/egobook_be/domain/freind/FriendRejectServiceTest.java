package com.example.egobook_be.domain.freind;
import com.example.egobook_be.domain.friend.entity.FriendRequest;
import com.example.egobook_be.domain.friend.enums.FriendRequestStatus;
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
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FriendRejectServiceTest {

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
    @DisplayName("rejectRequest_정상거절_성공")
    void rejectRequest_validRequest_success() {

        given(friendRequest.getSender()).willReturn(sender);
        given(userRepository.findById(2L)).willReturn(Optional.of(receiver));
        given(friendRequestRepository.findByIdAndReceiver(10L, receiver))
                .willReturn(Optional.of(friendRequest));
        given(friendRepository.existsByUserAndFriend(receiver, sender)).willReturn(false);
        given(friendRequest.getStatus()).willReturn(FriendRequestStatus.PENDING);

        friendService.rejectRequest(2L, 10L);

        verify(friendRequest).reject();
    }

    @Test
    @DisplayName("rejectRequest_수신자없음_실패")
    void rejectRequest_receiverNotFound_fail() {

        given(userRepository.findById(2L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> friendService.rejectRequest(2L, 10L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(FriendErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("rejectRequest_신청내역없음_실패")
    void rejectRequest_requestNotFound_fail() {

        given(userRepository.findById(2L)).willReturn(Optional.of(receiver));
        given(friendRequestRepository.findByIdAndReceiver(10L, receiver))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> friendService.rejectRequest(2L, 10L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(FriendErrorCode.FRIEND_REQUEST_NOT_FOUND);
    }

    @Test
    @DisplayName("rejectRequest_이미친구인경우_실패")
    void rejectRequest_alreadyFriend_fail() {

        given(friendRequest.getSender()).willReturn(sender);
        given(userRepository.findById(2L)).willReturn(Optional.of(receiver));
        given(friendRequestRepository.findByIdAndReceiver(10L, receiver))
                .willReturn(Optional.of(friendRequest));
        given(friendRepository.existsByUserAndFriend(receiver, sender)).willReturn(true);

        assertThatThrownBy(() -> friendService.rejectRequest(2L, 10L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(FriendErrorCode.ALREADY_FRIEND);
    }

    @Test
    @DisplayName("rejectRequest_이미처리된신청_실패")
    void rejectRequest_alreadyProcessedRequest_fail() {

        given(friendRequest.getSender()).willReturn(sender);
        given(userRepository.findById(2L)).willReturn(Optional.of(receiver));
        given(friendRequestRepository.findByIdAndReceiver(10L, receiver))
                .willReturn(Optional.of(friendRequest));
        given(friendRepository.existsByUserAndFriend(receiver, sender)).willReturn(false);
        given(friendRequest.getStatus()).willReturn(FriendRequestStatus.REJECTED);

        assertThatThrownBy(() -> friendService.rejectRequest(2L, 10L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(FriendErrorCode.INVALID_FRIEND_REQUEST_STATE);
    }

    @Test
    @DisplayName("rejectRequest_동시요청낙관적락충돌_실패")
    void rejectRequest_optimisticLockConflict_fail() {

        given(friendRequest.getSender()).willReturn(sender);
        given(userRepository.findById(2L)).willReturn(Optional.of(receiver));
        given(friendRequestRepository.findByIdAndReceiver(10L, receiver))
                .willReturn(Optional.of(friendRequest));
        given(friendRepository.existsByUserAndFriend(receiver, sender)).willReturn(false);
        given(friendRequest.getStatus()).willReturn(FriendRequestStatus.PENDING);
        doThrow(ObjectOptimisticLockingFailureException.class).when(friendRequest).reject();

        assertThatThrownBy(() -> friendService.rejectRequest(2L, 10L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(FriendErrorCode.FRIEND_REQUEST_CONFLICT);
    }
}
