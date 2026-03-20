package com.example.egobook_be.domain.notification;

import com.example.egobook_be.domain.notification.entity.Notification;
import com.example.egobook_be.domain.notification.enums.NotificationType;
import com.example.egobook_be.domain.notification.service.FcmService;
import com.example.egobook_be.domain.user.entity.User;
import com.example.egobook_be.domain.user.repository.UserRepository;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FcmServiceTest {

    @InjectMocks
    private FcmService fcmService;

    @Mock
    private UserRepository userRepository;

    private User user;
    private Notification notification;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).build();
        notification = Notification.builder()
                .title("새 편지")
                .content("편지 도착")
                .type(NotificationType.LETTER_NEW)
                .targetId(100L)
                .build();
    }

    @Test
    @DisplayName("FCM 토큰이 없으면 알림 전송하지 않고 종료")
    void sendPushNotification_NoToken_Success() {
        // Given - fcmToken 없음

        // When
        try (MockedStatic<FirebaseMessaging> mockedFirebase = mockStatic(FirebaseMessaging.class)) {
            fcmService.sendPushNotification(user, notification);

            // Then
            mockedFirebase.verify(FirebaseMessaging::getInstance, never());
        }
    }

    @Test
    @DisplayName("FCM 토큰이 있으면 알림 전송 성공")
    void sendPushNotification_Success() throws FirebaseMessagingException {
        // Given
        ReflectionTestUtils.setField(user, "fcmToken", "test-token-777");

        FirebaseMessaging messagingMock = mock(FirebaseMessaging.class);
        given(messagingMock.send(any(Message.class))).willReturn("projects/test/123");

        // When
        try (MockedStatic<FirebaseMessaging> mockedStatic = mockStatic(FirebaseMessaging.class)) {
            mockedStatic.when(FirebaseMessaging::getInstance).thenReturn(messagingMock);
            fcmService.sendPushNotification(user, notification);

            // Then
            verify(messagingMock, times(1)).send(any(Message.class));
        }
    }

    @Test
    @DisplayName("FCM 전송 중 예외 발생해도 중단되지 않고 로그 출력")
    void sendPushNotification_Exception_Handled() throws FirebaseMessagingException {
        // Given
        ReflectionTestUtils.setField(user, "fcmToken", "valid-token");

        FirebaseMessaging messagingMock = mock(FirebaseMessaging.class);
        FirebaseMessagingException exception = mock(FirebaseMessagingException.class);
        given(messagingMock.send(any(Message.class))).willThrow(exception);

        // When
        try (MockedStatic<FirebaseMessaging> mockedStatic = mockStatic(FirebaseMessaging.class)) {
            mockedStatic.when(FirebaseMessaging::getInstance).thenReturn(messagingMock);
            fcmService.sendPushNotification(user, notification);

            // Then
            verify(messagingMock, times(1)).send(any(Message.class));
        }
    }
}
