package com.example.egobook_be.domain.notification.controller;

import com.example.egobook_be.domain.notification.dto.NotificationResDto;
import com.example.egobook_be.domain.notification.service.NotificationService;
import com.example.egobook_be.global.response.GlobalResponse;
import com.example.egobook_be.global.response.SliceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(
                GlobalResponse.success(notificationService.getNotifications(userId, page, size))
        );
    }
}
