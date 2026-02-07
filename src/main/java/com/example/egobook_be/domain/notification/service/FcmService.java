package com.example.egobook_be.domain.notification.service;

import com.example.egobook_be.domain.notification.entity.Notification;
import com.example.egobook_be.domain.user.entity.User;
import com.example.egobook_be.domain.user.repository.UserRepository;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class FcmService {

    private final UserRepository userRepository;

    /** 푸시 알림 전송 */
    public void sendPushNotification(User user, Notification notification) {

        String fcmToken = user.getFcmToken();

        // FCM 토큰이 없으면 전송 안함
        if (fcmToken == null || fcmToken.isEmpty()) {
            log.warn("FCM token not found for user: {}", user.getId());
            return;
        }

        try {
            Message message = Message.builder()
                    .setToken(fcmToken)
                    .setNotification(com.google.firebase.messaging.Notification.builder()
                            .setTitle(notification.getTitle())
                            .setBody(notification.getContent())
                            .build())
                    .putData("targetId", String.valueOf(notification.getTargetId()))
                    .putData("type", notification.getType().name())
                    .build();

            String response = FirebaseMessaging.getInstance().send(message);
            log.info("Successfully sent FCM message to user {}: {}", user.getId(), response);

        } catch (FirebaseMessagingException e) {
            log.error("Failed to send FCM message to user {}", user.getId(), e);
        }
    }
}
