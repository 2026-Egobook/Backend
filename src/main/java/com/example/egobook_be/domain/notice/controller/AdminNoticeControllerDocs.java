package com.example.egobook_be.domain.notice.controller;

import com.example.egobook_be.domain.notice.dto.NoticeAdminResDto;
import com.example.egobook_be.domain.notice.dto.NoticeCreateReqDto;
import com.example.egobook_be.domain.notice.dto.NoticeUpdateReqDto;
import com.example.egobook_be.global.response.GlobalResponse;
import com.example.egobook_be.global.response.SliceResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Admin Notice", description = "공지사항 관리자 API")
@RequestMapping("/admin/notices")
public interface AdminNoticeControllerDocs {

    @Operation(summary = "[관리자] 공지사항 등록", description = """
            공지사항을 등록합니다. 노션 링크를 등록하면 앱에서는 웹뷰로 해당 페이지를 띄웁니다.

            [Request Body]
            - title: 공지 제목
            - notionUrl: 공지 노션 페이지 링크
            - publishedAt: 발행(노출 시작) 일시. 이 시각이 지나야 유저에게 노출/레드닷이 뜹니다.
            """)
    @SecurityRequirement(name = "bearerAuth")
    ResponseEntity<GlobalResponse<NoticeAdminResDto>> createNotice(@RequestBody NoticeCreateReqDto reqDto);

    @Operation(summary = "[관리자] 공지사항 목록 조회", description = "공지사항 목록을 발행일시 최신순으로 조회합니다.")
    @SecurityRequirement(name = "bearerAuth")
    ResponseEntity<GlobalResponse<SliceResponse<NoticeAdminResDto>>> getNotices(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    );

    @Operation(summary = "[관리자] 공지사항 수정", description = """
            공지사항을 수정합니다.
            삭제 후 재등록하면 이미 확인한 유저들에게 레드닷이 다시 뜨므로, 오탈자 등을 고칠 때는 이 API를 사용하세요.
            """)
    @SecurityRequirement(name = "bearerAuth")
    ResponseEntity<GlobalResponse<NoticeAdminResDto>> updateNotice(
            @PathVariable Long noticeId,
            @RequestBody NoticeUpdateReqDto reqDto
    );

    @Operation(summary = "[관리자] 공지사항 삭제")
    @SecurityRequirement(name = "bearerAuth")
    ResponseEntity<GlobalResponse<Void>> deleteNotice(@PathVariable Long noticeId);
}
