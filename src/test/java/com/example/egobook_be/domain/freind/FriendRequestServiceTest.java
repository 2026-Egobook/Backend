package com.example.egobook_be.domain.freind;

import com.example.egobook_be.domain.friend.dto.FriendRequestCreateReqDto;
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
import org.mockito.ArgumentCaptor;
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
class FriendRequestServiceTest {

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
    private FriendRequestCreateReqDto reqDto;

    @BeforeEach
    void setUp() {
        sender = mock(User.class);
        receiver = mock(User.class);

        reqDto = FriendRequestCreateReqDto.builder()
                .receiverId(2L)
                .build();
    }

    @Test
    @DisplayName("requestFriend_정상신청_성공")
    void requestFriend_validRequest_success() {

        given(userRepository.findById(1L)).willReturn(Optional.of(sender));
        given(userRepository.findById(2L)).willReturn(Optional.of(receiver));
        given(friendRepository.existsByUserAndFriend(sender, receiver)).willReturn(false);
        given(friendRepository.existsByUserAndFriend(receiver, sender)).willReturn(false);
        given(friendRequestRepository.findBySenderAndReceiver(sender, receiver))
                .willReturn(Optional.empty());

        friendService.requestFriend(1L, reqDto);

        ArgumentCaptor<FriendRequest> captor = ArgumentCaptor.forClass(FriendRequest.class);
        verify(friendRequestRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(FriendRequestStatus.PENDING);
    }

    @Test
    @DisplayName("requestFriend_자기자신에게신청_실패")
    void requestFriend_selfRequest_fail() {

        FriendRequestCreateReqDto selfReqDto = FriendRequestCreateReqDto.builder()
                .receiverId(1L)
                .build();

        assertThatThrownBy(() -> friendService.requestFriend(1L, selfReqDto))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(FriendErrorCode.SELF_REQUEST_NOT_ALLOWED);
    }

    @Test
    @DisplayName("requestFriend_발신자없음_실패")
    void requestFriend_senderNotFound_fail() {

        given(userRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> friendService.requestFriend(1L, reqDto))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(FriendErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("requestFriend_수신자없음_실패")
    void requestFriend_receiverNotFound_fail() {

        given(userRepository.findById(1L)).willReturn(Optional.of(sender));
        given(userRepository.findById(2L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> friendService.requestFriend(1L, reqDto))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(FriendErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("requestFriend_이미친구인경우_실패")
    void requestFriend_alreadyFriend_fail() {

        given(userRepository.findById(1L)).willReturn(Optional.of(sender));
        given(userRepository.findById(2L)).willReturn(Optional.of(receiver));
        given(friendRepository.existsByUserAndFriend(sender, receiver)).willReturn(true);

        assertThatThrownBy(() -> friendService.requestFriend(1L, reqDto))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(FriendErrorCode.ALREADY_FRIEND);
    }

    @Test
    @DisplayName("requestFriend_역방향이미친구인경우_실패")
    void requestFriend_alreadyFriendReverse_fail() {

        given(userRepository.findById(1L)).willReturn(Optional.of(sender));
        given(userRepository.findById(2L)).willReturn(Optional.of(receiver));
        given(friendRepository.existsByUserAndFriend(sender, receiver)).willReturn(false);
        given(friendRepository.existsByUserAndFriend(receiver, sender)).willReturn(true);

        assertThatThrownBy(() -> friendService.requestFriend(1L, reqDto))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(FriendErrorCode.ALREADY_FRIEND);
    }

    @Test
    @DisplayName("requestFriend_이미PENDING신청존재_실패")
    void requestFriend_alreadyPendingRequest_fail() {

        FriendRequest existingRequest = mock(FriendRequest.class);
        given(existingRequest.getStatus()).willReturn(FriendRequestStatus.PENDING);

        given(userRepository.findById(1L)).willReturn(Optional.of(sender));
        given(userRepository.findById(2L)).willReturn(Optional.of(receiver));
        given(friendRepository.existsByUserAndFriend(sender, receiver)).willReturn(false);
        given(friendRepository.existsByUserAndFriend(receiver, sender)).willReturn(false);
        given(friendRequestRepository.findBySenderAndReceiver(sender, receiver))
                .willReturn(Optional.of(existingRequest));

        assertThatThrownBy(() -> friendService.requestFriend(1L, reqDto))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(FriendErrorCode.ALREADY_REQUESTED);
    }

    @Test
    @DisplayName("requestFriend_ACCEPTED상태신청재시도_실패")
    void requestFriend_retryOnAcceptedRequest_fail() {

        FriendRequest existingRequest = mock(FriendRequest.class);
        given(existingRequest.getStatus()).willReturn(FriendRequestStatus.ACCEPTED);

        given(userRepository.findById(1L)).willReturn(Optional.of(sender));
        given(userRepository.findById(2L)).willReturn(Optional.of(receiver));
        given(friendRepository.existsByUserAndFriend(sender, receiver)).willReturn(false);
        given(friendRepository.existsByUserAndFriend(receiver, sender)).willReturn(false);
        given(friendRequestRepository.findBySenderAndReceiver(sender, receiver))
                .willReturn(Optional.of(existingRequest));

        assertThatThrownBy(() -> friendService.requestFriend(1L, reqDto))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(FriendErrorCode.ALREADY_FRIEND);
    }

    @Test
    @DisplayName("requestFriend_REJECTED후재신청_성공")
    void requestFriend_reRequestAfterRejected_success() {

        FriendRequest existingRequest = mock(FriendRequest.class);
        given(existingRequest.getStatus()).willReturn(FriendRequestStatus.REJECTED);

        given(userRepository.findById(1L)).willReturn(Optional.of(sender));
        given(userRepository.findById(2L)).willReturn(Optional.of(receiver));
        given(friendRepository.existsByUserAndFriend(sender, receiver)).willReturn(false);
        given(friendRepository.existsByUserAndFriend(receiver, sender)).willReturn(false);
        given(friendRequestRepository.findBySenderAndReceiver(sender, receiver))
                .willReturn(Optional.of(existingRequest));

        friendService.requestFriend(1L, reqDto);

        verify(existingRequest).reRequest();
        verify(friendRequestRepository, never()).save(any(FriendRequest.class));
    }
}
