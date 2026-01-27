package com.example.egobook_be.domain.ego_room.controller;


import com.example.egobook_be.domain.ego_room.dto.*;
import com.example.egobook_be.domain.ego_room.service.EgoRoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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

    @Operation(summary = "일간 칭찬서 목록 조회", description = "일간 칭찬서들의 날짜를 보여줍니다.")
    @GetMapping("/praise/daily")
    public ResponseEntity<DailyPraiseListResDto> getDailyPraiseList(
            @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId,
            @Parameter(description = "마지막으로 조회된 항목의 ID (0일 경우 처음부터 조회)", example = "0")
            @RequestParam(defaultValue = "0") Long cursor,
            @Parameter(description = "한 번에 가져올 항목 개수", example = "30")
            @RequestParam(defaultValue = "30") int size
    ) {
        return ResponseEntity.ok(egoRoomService.getDailyPraiseList(userId, cursor, size));
    }

    @Operation(summary = "일간 칭찬서 상세 조회", description = "특정 날짜의 칭찬서 내용을 확인하고 최초 열람 시 자존감을 1 상승시킵니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
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
    public ResponseEntity<WeeklyCounselListResDto> getWeeklyCounselList(
            @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId,
            @Parameter(description = "마지막 조회 ID", example = "0")
            @RequestParam(defaultValue = "0") Long cursor,
            @Parameter(description = "조회 개수", example = "20")
            @RequestParam(defaultValue = "20") int size
    ) {

        return ResponseEntity.ok(egoRoomService.getWeeklyCounselList(userId, cursor, size));
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
    @Operation(summary = "다음 주 상담 분위기 변경", description = "다음 주에 생성될 AI 상담서의 말투나 스타일을 미리 설정합니다")
    @PatchMapping("/counsel/weekly/next-tone")
    public ResponseEntity<CounselToneResDto> updateNextWeekTone(
            @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId,
            @RequestBody CounselTonePatchReqDto reqDto
    ) {
        return ResponseEntity.ok(egoRoomService.updateNextWeekTone(userId, reqDto.toneStyle()));
    }
}