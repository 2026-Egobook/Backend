package com.example.egobook_be.domain.notification;

import com.example.egobook_be.domain.letters.entity.PlazaLetterReply;
import com.example.egobook_be.domain.letters.enums.LettersErrorCode;
import com.example.egobook_be.domain.letters.repository.PlazaLetterReplyRepository;
import com.example.egobook_be.domain.letters.repository.PlazaLetterRepository;
import com.example.egobook_be.domain.notification.entity.Notification;
import com.example.egobook_be.domain.notification.enums.NotificationType;
import com.example.egobook_be.domain.notification.repository.NotificationRepository;
import com.example.egobook_be.domain.notification.service.FcmService;
import com.example.egobook_be.domain.notification.service.NotificationService;
import com.example.egobook_be.domain.user.entity.User;
import com.example.egobook_be.domain.user.repository.UserRepository;
import com.example.egobook_be.global.exception.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @InjectMocks
    private NotificationService notificationService;

    @Mock private UserRepository userRepository;
    @Mock private NotificationRepository notificationRepository;
    @Mock private PlazaLetterRepository plazaLetterRepository;
    @Mock private PlazaLetterReplyRepository plazaLetterReplyRepository;
    @Mock private FcmService fcmService;

    private final Long USER_ID = 1L;
    private final Long TARGET_ID = 100L;

    @Test
    @DisplayName("답장 알림 생성 성공 - 미리보기 17자 잘림 및 FCM 전송")
    void createNotification_Reply_Success() {
        // Given
        User user = User.builder().build();
        ReflectionTestUtils.setField(user, "notificationEnabled", true);
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));

        String longContent = "17자가 넘는지 확인. 힘들었겠따... 근데 나도 힘들어";
        PlazaLetterReply reply = PlazaLetterReply.builder().content(longContent).build();
        given(plazaLetterReplyRepository.findById(TARGET_ID)).willReturn(Optional.of(reply));

        // When
        notificationService.createNotification(USER_ID, NotificationType.LETTER_REPLY_FRIEND, TARGET_ID, "짱");

        // Then
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());

        Notification savedNotification = captor.getValue();
        assertThat(savedNotification.getType()).isEqualTo(NotificationType.LETTER_REPLY_FRIEND);
        assertThat(savedNotification.getTitle()).contains("짱");
        assertThat(savedNotification.getContent()).hasSize(17);
        assertThat(savedNotification.getContent()).isEqualTo(longContent.substring(0, 17));

        verify(fcmService).sendPushNotification(eq(user), any(Notification.class));
    }

    @Test
    @DisplayName("알림 설정이 꺼져있으면 알림 생성 X")
    void createNotification_Disabled_Success() {
        // Given
        User user = User.builder().build();
        ReflectionTestUtils.setField(user, "notificationEnabled", false);
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));

        // When
        notificationService.createNotification(USER_ID, NotificationType.LETTER_NEW, TARGET_ID);

        // Then
        verify(notificationRepository, never()).save(any());
        verify(fcmService, never()).sendPushNotification(any(), any());
    }

    @Test
    @DisplayName("알림 생성 시 유저가 존재하지 않으면 예외 발생")
    void createNotification_UserNotFound_Fail() {
        // Given
        given(userRepository.findById(999L)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() ->
                notificationService.createNotification(999L, NotificationType.LETTER_NEW, TARGET_ID))
                .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("알림 생성 시 해당 편지가 존재하지 않으면 예외 발생")
    void createNotification_LetterNotFound_Fail() {
        // Given
        User user = User.builder().build();
        ReflectionTestUtils.setField(user, "notificationEnabled", true);
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
        given(plazaLetterRepository.findById(TARGET_ID)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() ->
                notificationService.createNotification(USER_ID, NotificationType.LETTER_NEW, TARGET_ID))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", LettersErrorCode.LETTER_NOT_FOUND);
    }
}
