package com.example.egobook_be.domain.notification.controller;

import com.example.egobook_be.domain.notification.dto.NotificationResDto;
import com.example.egobook_be.global.response.GlobalResponse;
import com.example.egobook_be.global.response.SliceResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Notification Controller", description = "알림 관련 API")
public interface NotificationControllerDocs {

    @Operation(summary = "알림 목록 조회", description = """
            알림은 최신순으로 확인 가능합니다.
            
            - 편지 답장
            - 친구에게서 온 편지
            - AI 칭찬서
            - AI 상담서
            """)
    @GetMapping
    ResponseEntity<GlobalResponse<SliceResponse<NotificationResDto>>> getNotifications(
            @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    );
}
