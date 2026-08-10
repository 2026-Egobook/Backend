package com.example.egobook_be.domain.notice.controller;

import com.example.egobook_be.domain.notice.dto.NoticeLatestResDto;
import com.example.egobook_be.global.response.GlobalResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "Notice", description = "공지사항 관련 API")
@RequestMapping("/notices")
public interface NoticeControllerDocs {

    @Operation(
            summary = "최신 공지사항 조회",
            description = "현재 노출 중인 공지 중 가장 최근 1건을 조회합니다. 조회와 동시에 해당 유저의 읽음 처리가 이루어집니다 (레드닷 판단용, /home 응답의 hasUnreadNotice에 반영됨). 발행된 공지가 하나도 없으면 data는 null입니다."
    )
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/latest")
    ResponseEntity<GlobalResponse<NoticeLatestResDto>> getLatestNotice(
            @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId
    );
}
