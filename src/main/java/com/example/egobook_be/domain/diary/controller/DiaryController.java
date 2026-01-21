package com.example.egobook_be.domain.diary.controller;

import com.example.egobook_be.domain.diary.dto.*;
import com.example.egobook_be.domain.diary.enums.DiaryType;
import com.example.egobook_be.domain.diary.service.DiaryService;
import com.example.egobook_be.global.response.GlobalResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;

@RestController
@RequiredArgsConstructor
@RequestMapping("/diaries")
public class DiaryController implements DiaryControllerDocs{

    private final DiaryService diaryService;

    /**
     * [감정 일기 생성]
     * POST /diaries
     */
    @Override
    public ResponseEntity<GlobalResponse<DiaryCreateResDto>> createDiary(
            @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId,
            @RequestBody @Valid DiaryCreateReqDto dto
    ) {
        return ResponseEntity.ok(
                GlobalResponse.success(diaryService.createDiary(userId, dto))
        );
    }

    /**
     * [작성된 감정 일기 목록]
     * GET /diaries
     */
    @Override
    public ResponseEntity<GlobalResponse<DiaryListResDto>> getDiaries(
            @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) DiaryType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(
                GlobalResponse.success(diaryService.getDiaries(userId, date, type, page, size))
        );
    }

    /**
     * [작성된 감정 일기 상세 확인]
     * GET /diaries/{diaryId}
     */
    @Override
    public ResponseEntity<GlobalResponse<DiaryResDto>> getDiary(
            @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId,
            @PathVariable Long diaryId
    ) {
        return ResponseEntity.ok(
                GlobalResponse.success(diaryService.getDiary(userId, diaryId))
        );
    }

    /**
     * [감정 일기 수정]
     * PATCH /diaries/{diaryId}
     */
    @Override
    public ResponseEntity<GlobalResponse<DiaryResDto>> updateDiary(
            @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId,
            @PathVariable Long diaryId,
            @RequestBody @Valid DiaryUpdateReqDto dto
    ) {
        return ResponseEntity.ok(
                GlobalResponse.success(diaryService.updateDiary(userId, diaryId, dto))
        );
    }

    /**
     * [감정 일기 삭제]
     * DELETE /diaries/{diaryId}
     */
    @Override
    public ResponseEntity<GlobalResponse<Boolean>> deleteDiary(
            @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId,
            @PathVariable Long diaryId
    ) {
        diaryService.deleteDiary(userId, diaryId);
        return ResponseEntity.ok(
                GlobalResponse.success("deleted", true)
        );
    }

    /**
     * [감정 일기 달력]
     * GET /diaries/calender
     */
    @Override
    public ResponseEntity<GlobalResponse<DiaryCalendarResDto>> getDiaryCalendar(
            @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId,
            @RequestParam
            @DateTimeFormat(pattern = "yyyy-MM") YearMonth month
    ) {
        return ResponseEntity.ok(
                GlobalResponse.success(diaryService.getDiaryCalendar(userId, month))
        );
    }

    /**
     * [감정 일기 내보내기]
     * POST /diaries/export
     */
    @Override
    public ResponseEntity<GlobalResponse<DiaryExportResDto>> exportDiaries(
            @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId,
            @RequestBody @Valid DiaryExportReqDto dto
    ) {
        return ResponseEntity.ok(
                GlobalResponse.success(diaryService.exportDiaries(userId, dto))
        );
    }
}
