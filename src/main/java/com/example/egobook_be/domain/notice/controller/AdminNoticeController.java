package com.example.egobook_be.domain.notice.controller;

import com.example.egobook_be.domain.notice.dto.NoticeAdminResDto;
import com.example.egobook_be.domain.notice.dto.NoticeCreateReqDto;
import com.example.egobook_be.domain.notice.dto.NoticeReadResetResDto;
import com.example.egobook_be.domain.notice.dto.NoticeUpdateReqDto;
import com.example.egobook_be.domain.notice.service.AdminNoticeService;
import com.example.egobook_be.global.response.GlobalResponse;
import com.example.egobook_be.global.response.SliceResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/notices")
public class AdminNoticeController implements AdminNoticeControllerDocs {

    private final AdminNoticeService adminNoticeService;

    @Override
    @PostMapping
    public ResponseEntity<GlobalResponse<NoticeAdminResDto>> createNotice(@Valid @RequestBody NoticeCreateReqDto reqDto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GlobalResponse.success(201, "공지사항 등록 성공", adminNoticeService.createNotice(reqDto)));
    }

    @Override
    @GetMapping
    public ResponseEntity<GlobalResponse<SliceResponse<NoticeAdminResDto>>> getNotices(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(
                GlobalResponse.success("공지사항 목록 조회 성공", adminNoticeService.getNotices(page, size))
        );
    }

    @Override
    @PatchMapping("/{noticeId}")
    public ResponseEntity<GlobalResponse<NoticeAdminResDto>> updateNotice(
            @PathVariable Long noticeId,
            @Valid @RequestBody NoticeUpdateReqDto reqDto
    ) {
        return ResponseEntity.ok(
                GlobalResponse.success("공지사항 수정 성공", adminNoticeService.updateNotice(noticeId, reqDto))
        );
    }

    @Override
    @DeleteMapping("/{noticeId}")
    public ResponseEntity<GlobalResponse<Void>> deleteNotice(@PathVariable Long noticeId) {
        adminNoticeService.deleteNotice(noticeId);
        return ResponseEntity.ok(GlobalResponse.success("공지사항 삭제 성공", null));
    }

    @Override
    @PostMapping("/reads/reset")
    public ResponseEntity<GlobalResponse<NoticeReadResetResDto>> resetNoticeReads() {
        return ResponseEntity.ok(
                GlobalResponse.success("공지 읽음 기록 초기화 성공", adminNoticeService.resetNoticeReads())
        );
    }
}
