package com.example.egobook_be.domain.notification.controller;

import com.example.egobook_be.domain.notification.dto.NotificationReadResDto;
import com.example.egobook_be.domain.notification.dto.NotificationResDto;
import com.example.egobook_be.domain.notification.dto.NotificationSettingResDto;
import com.example.egobook_be.domain.notification.service.NotificationService;
import com.example.egobook_be.global.response.GlobalResponse;
import com.example.egobook_be.global.response.SliceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/notifications")
public class NotificationController implements NotificationControllerDocs{

    private final NotificationService notificationService;

    /**
     * [알림 목록 조회]
     * GET /notifications
     */
    @Override
    public ResponseEntity<GlobalResponse<SliceResponse<NotificationResDto>>> getNotifications(
            @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(
                GlobalResponse.success(notificationService.getNotifications(userId, page, size))
        );
    }

    /**
     * [알림 읽음]
     * POST /notifications/{notificationId}/read
     */
    @Override
    public ResponseEntity<GlobalResponse<NotificationReadResDto>> readNotification(
            @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId,
            @PathVariable Long notificationId
    ) {
        return ResponseEntity.ok(
                GlobalResponse.success(notificationService.readNotification(userId, notificationId))
        );
    }

    /**
     * [알림 설정 확인]
     * GET /notifications/settings
     */
    @Override
    public ResponseEntity<GlobalResponse<NotificationSettingResDto>> getNotificationSetting(
            @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId
    ) {
        return ResponseEntity.ok(
                GlobalResponse.success(notificationService.getNotificationSetting(userId))
        );
    }

    /**
     * [알림 설정 변경]
     * PATCH /notifications/settings
     */
    @Override
    public ResponseEntity<GlobalResponse<NotificationSettingResDto>> updateNotificationSetting(
            @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId
    ) {
        return ResponseEntity.ok(
                GlobalResponse.success(notificationService.updateNotificationSetting(userId))
        );
    }
}
