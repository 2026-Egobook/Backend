package com.example.egobook_be.domain.ego_room.controller;


import com.example.egobook_be.domain.ego_room.dto.*;
import com.example.egobook_be.domain.ego_room.service.EgoRoomService;
import com.example.egobook_be.domain.ego_room.service.EgoStatsService;
import com.example.egobook_be.global.response.SliceResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import com.example.egobook_be.global.response.GlobalResponse;
import com.example.egobook_be.global.response.SliceResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Tag(name = "EgoRoom Controller", description = "일간 칭찬서 및 주간 상담서 관리 API")
@RestController
@RequestMapping("/ego-room")
@RequiredArgsConstructor
public class EgoRoomController {

    private final EgoRoomService egoRoomService;
    private final EgoStatsService egoRoomStatService;

    @Operation(summary = "일간 칭찬서 목록 조회", description = "일간 칭찬서들의 날짜를 보여줍니다.")
    @GetMapping("/praise/daily")
    public GlobalResponse<SliceResponse<DailyPraiseSimpleItemDto>> getDailyPraiseList(
            @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "30") int size
    ) {
        return GlobalResponse.success(egoRoomService.getDailyPraiseList(userId, page, size));
    }

    @Operation(summary = "일간 칭찬서 상세 조회", description = "특정 날짜의 칭찬서 내용을 확인하고 최초 열람 시 자존감을 1 상승시킵니다.")
    @GetMapping("/praise/daily/{date}")
    public ResponseEntity<DailyPraiseItemDto> getDailyPraiseDetail(
            @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId,
            @Parameter(description = "조회할 날짜 (yyyy-MM-dd)", example = "2026-01-26")
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ResponseEntity.ok(egoRoomService.getDailyPraiseDetail(userId, date));
    }

    @Operation(summary = "주간 상담서 목록 조회", description = "사용자에게 발행된 주간 상담서 목록을 조회합니다.")
    @GetMapping("/counsel/weekly")
    public GlobalResponse<SliceResponse<WeeklyCounselSimpleItemDto>> getWeeklyCounselList(
            @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        // 서비스 메서드에 page와 size를 순서대로 넘겨줘
        return GlobalResponse.success(egoRoomService.getWeeklyCounselList(userId, page, size));
    }

    @Operation(summary = "주간 상담서 상세 조회", description = "특정 주의 시작 날짜(월요일)로 상담서 상세 내용을 조회합니다.")
    @GetMapping("/counsel/weekly/{startDate}")
    public ResponseEntity<WeeklyCounselDetailResDto> getWeeklyCounselDetail(
            @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId,
            @Parameter(description = "조회할 주의 시작 날짜 (yyyy-MM-dd)", example = "2026-01-26")
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate
    ) {
        return ResponseEntity.ok(egoRoomService.getWeeklyCounselDetail(userId, startDate));
    }
    @Operation(summary = "다음 주 상담 분위기 변경", description = "다음 주에 생성될 AI 상담서의 말투나 스타일을 미리 설정합니다. SHARP OBJECTIVE SOFT")
    @PatchMapping("/counsel/weekly/next-tone")
    public ResponseEntity<CounselToneResDto> updateNextWeekTone(
            @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId,
            @RequestBody CounselTonePatchReqDto reqDto
    ) {
        return ResponseEntity.ok(egoRoomService.updateNextWeekTone(userId, reqDto.toneStyle()));
    }

    @Operation(summary = "통계 조회", description = "입력 월을 포함하는 1년 간의 감정 통계 그래프 데이터를 조회합니다.")
    @GetMapping("/stats")
    public ResponseEntity<EgoStatsResDto> getMonthlyStats(
            @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId
    ) {
        return ResponseEntity.ok(egoRoomStatService.getStats(userId));
    }
}