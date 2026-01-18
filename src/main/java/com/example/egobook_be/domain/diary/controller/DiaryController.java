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

@RestController
@RequiredArgsConstructor
@RequestMapping("/diaries")
public class DiaryController {

    private final DiaryService diaryService;

    @PostMapping("")
    public ResponseEntity<GlobalResponse<DiaryCreateResDto>> createDiary(
            @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId,
            @RequestBody @Valid DiaryCreateReqDto dto
    ) {
        return ResponseEntity.ok(
                GlobalResponse.success(diaryService.createDiary(userId, dto))
        );
    }

    @GetMapping("")
    public ResponseEntity<GlobalResponse<DiaryListResDto>> getDiaries(
            @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) DiaryType type
    ) {
        return ResponseEntity.ok(
                GlobalResponse.success(diaryService.getDiaries(userId, date, type))
        );
    }

    @GetMapping("/{diaryId}")
    public ResponseEntity<GlobalResponse<DiaryResDto>> getDiary(
            @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId,
            @PathVariable Long diaryId
    ) {
        return ResponseEntity.ok(
                GlobalResponse.success(diaryService.getDiary(userId, diaryId))
        );
    }

    @PatchMapping("/{diaryId}")
    public ResponseEntity<GlobalResponse<DiaryResDto>> updateDiary(
            @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId,
            @PathVariable Long diaryId,
            @RequestBody @Valid DiaryUpdateReqDto dto
    ) {
        return ResponseEntity.ok(
                GlobalResponse.success(diaryService.updateDiary(userId, diaryId, dto))
        );
    }

    @DeleteMapping("/{diaryId}")
    public ResponseEntity<GlobalResponse<Boolean>> deleteDiary(
            @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId,
            @PathVariable Long diaryId
    ) {
        diaryService.deleteDiary(userId, diaryId);
        return ResponseEntity.ok(
                GlobalResponse.success("deleted", true)
        );
    }
}
