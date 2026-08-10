package com.example.egobook_be.domain.notice.controller;

import com.example.egobook_be.domain.notice.dto.NoticeLatestResDto;
import com.example.egobook_be.domain.notice.service.NoticeService;
import com.example.egobook_be.global.response.GlobalResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class NoticeController implements NoticeControllerDocs {

    private final NoticeService noticeService;

    @Override
    public ResponseEntity<GlobalResponse<NoticeLatestResDto>> getLatestNotice(
            @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId
    ) {
        return ResponseEntity.ok(
                GlobalResponse.success("최신 공지사항 조회 성공", noticeService.getLatestNotice(userId))
        );
    }
}
