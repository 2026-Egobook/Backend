package com.example.egobook_be.domain.ego_room.controller;


import com.example.egobook_be.domain.ego_room.dto.*;
import com.example.egobook_be.domain.ego_room.enums.UnlockType;
import com.example.egobook_be.domain.ego_room.service.EgoRoomService;
import com.example.egobook_be.domain.ego_room.service.EgoStatsService;
import com.example.egobook_be.domain.user.entity.User;
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
import java.util.HashMap;
import java.util.Map;

@Tag(name = "EgoRoom Controller", description = "일간 칭찬서 및 주간 상담서 관리 API")
@RestController
@RequestMapping("/ego-room")
@RequiredArgsConstructor
public class EgoRoomController {

    private final EgoRoomService egoRoomService;
    private final EgoStatsService egoRoomStatService;

    @Operation(summary = "일간칭찬/주간보고서 수신 여부 get", description = "토글 on/off 정보를 받아옵니다")
    @GetMapping("/ai/toggle")
    public GlobalResponse<Map<String, Boolean>> getSettings(
            @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId
    ) {
        return GlobalResponse.success(egoRoomService.getSettings(userId));
    }

    @Operation(summary = "일간칭찬서 수신 여부 설정", description = "일간칭찬서 수신 여부를 설정합니다")
    @PatchMapping("/praise/daily")
    public GlobalResponse<Map<String, Boolean>> toggleDailyPraise(
            @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @io.swagger.v3.oas.annotations.media.Content(
                    schema = @io.swagger.v3.oas.annotations.media.Schema(example = "{\"enabled\": true}")
            ))
            @RequestBody Map<String, Boolean> payload) {

        Boolean enabled = payload.get("enabled");
        egoRoomService.updateDailyPraiseSetting(userId, enabled);

        return GlobalResponse.success(Map.of("enabled", enabled));
    }

    @Operation(summary = "주간보고서 수신 여부 설정", description = "주간보고서 수신 여부를 설정합니다")
    @PatchMapping("/counsel/weekly")
    public GlobalResponse<Map<String, Boolean>> toggleWeeklyAnalysis(
            @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @io.swagger.v3.oas.annotations.media.Content(
                    schema = @io.swagger.v3.oas.annotations.media.Schema(example = "{\"enabled\": true}")
            ))
            @RequestBody Map<String, Boolean> payload) {

        Boolean enabled = payload.get("enabled");
        egoRoomService.updateWeeklyAnalysisSetting(userId, enabled);

        return GlobalResponse.success(Map.of("enabled", enabled));
    }


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

    @Operation(summary = "주간 상담서 상세 조회", description = "특정 주의 시작 날짜(월요일)로 상담서 상세 내용을 조회합니다. 잠금 상태인 경우 데이터가 제한될 수 있습니다.")
    @GetMapping("/counsel/weekly/{startDate}")
    public ResponseEntity<WeeklyCounselDetailResDto> getWeeklyCounselDetail(
            @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId,
            @Parameter(description = "조회할 주의 시작 날짜 (yyyy-MM-dd)", example = "2026-01-26")
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate
    ) {
        return ResponseEntity.ok(egoRoomService.getWeeklyCounselDetail(userId, startDate));
    }

    @Operation(summary = "주간 상담서 잠금 해제", description = "잉크 10개 소모 또는 광고 시청을 통해 상담서를 해제합니다.")
    @PostMapping("/counsel/weekly/{startDate}/unlock")
    public GlobalResponse<String> unlockWeeklyCounsel(
            @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam UnlockType unlockType // INK 또는 AD
    ) {
        egoRoomService.unlockWeeklyCounsel(userId, startDate, unlockType);
        return GlobalResponse.success("잠금이 해제되었습니다.");
    }

    @Operation(summary = "상담서 분위기 조회", description = "현재 유저가 설정한 AI 상담 말투를 조회합니다.")
    @GetMapping("/counseling-tone")
    public GlobalResponse<String> getCounselTone(
            @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId
    ) {
        String counselTone = egoRoomService.getUserCounselTone(userId);
        return GlobalResponse.success(counselTone);
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