package com.example.egobook_be.domain.notification.controller;

import com.example.egobook_be.domain.notification.dto.NotificationReadResDto;
import com.example.egobook_be.domain.notification.dto.NotificationResDto;
import com.example.egobook_be.domain.notification.dto.NotificationSettingResDto;
import com.example.egobook_be.global.response.GlobalResponse;
import com.example.egobook_be.global.response.SliceResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "Notification Controller", description = "알림 관련 API")
public interface NotificationControllerDocs {

    @Operation(summary = "알림 목록 조회", description = """
            알림은 최신순으로 확인 가능합니다.
            
            - 편지 답장
            - 친구에게 온 편지
            - AI 칭찬서
            - AI 상담서
            """)
    @GetMapping
    ResponseEntity<GlobalResponse<SliceResponse<NotificationResDto>>> getNotifications(
            @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    );

    @Operation(summary = "알림 읽음", description = """
            이벤트가 발생할 경우 자동 생성합니다.
            
            - 편지 답장
            - 친구에게 온 편지
            - AI 칭찬서
            - AI 상담서
            """)
    @PostMapping("{notificationId}/read")
    ResponseEntity<GlobalResponse<NotificationReadResDto>> readNotification(
            @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId,
            @PathVariable Long notificationId
    );

    @Operation(summary = "알림 설정 확인", description = "알림 ON/OFF 설정을 확인합니다.")
    @GetMapping("/settings")
    ResponseEntity<GlobalResponse<NotificationSettingResDto>> getNotificationSetting(
            @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId
    );

    @Operation(summary = "알림 설정 변경", description = "알림 ON/OFF 설정을 변경합니다.")
    @PatchMapping("/settings")
    ResponseEntity<GlobalResponse<NotificationSettingResDto>> updateNotificationSetting(
            @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId
    );
}
