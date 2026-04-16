package com.example.egobook_be.domain.user.controller;

import com.example.egobook_be.domain.letters.enums.BlockType;
import com.example.egobook_be.domain.user.dto.ResendReqDto;
import com.example.egobook_be.domain.user.dto.AdminContentResDto.*;
import com.example.egobook_be.global.response.GlobalResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Tag(name = "Admin Content Controller", description = "콘텐츠 관리 관리자 API")
@RequestMapping("/admin")
public interface AdminContentControllerDocs {

    @Operation(summary = "AI 일간 칭찬서 발송 현황 조회")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "일간 칭찬서 발송 현황 조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 날짜 범위", content = @Content),
            @ApiResponse(responseCode = "401", description = "인증 필요", content = @Content),
            @ApiResponse(responseCode = "403", description = "관리자 권한 필요", content = @Content)
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/ai/daily-praise/status")
    ResponseEntity<GlobalResponse<DailyPraiseStatusRes>> getDailyPraiseStatus(
            @Parameter(description = "조회 시작일 (yyyy-MM-dd)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "조회 종료일 (yyyy-MM-dd)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    );

    @Operation(summary = "AI 일간 칭찬서 실패 건 수동 재발송")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "일간 칭찬서 재발송 처리 완료"),
            @ApiResponse(responseCode = "400", description = "failIds가 비어 있음", content = @Content),
            @ApiResponse(responseCode = "403", description = "관리자 권한 필요", content = @Content)
    })
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/ai/daily-praise/resend")
    ResponseEntity<GlobalResponse<ResendRes>> resendDailyPraise(
            @RequestBody ResendReqDto reqDto
    );

    @Operation(summary = "AI 주간 리포트 발송 현황 조회")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "주간 리포트 발송 현황 조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 날짜 범위", content = @Content),
            @ApiResponse(responseCode = "403", description = "관리자 권한 필요", content = @Content)
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/ai/weekly-report/status")
    ResponseEntity<GlobalResponse<WeeklyReportStatusRes>> getWeeklyReportStatus(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    );

    @Operation(summary = "AI 주간 리포트 실패 건 수동 재발송")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "주간 리포트 재발송 처리 완료"),
            @ApiResponse(responseCode = "400", description = "failIds가 비어 있음", content = @Content),
            @ApiResponse(responseCode = "403", description = "관리자 권한 필요", content = @Content)
    })
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/ai/weekly-report/resend")
    ResponseEntity<GlobalResponse<ResendRes>> resendWeeklyReport(
            @RequestBody ResendReqDto reqDto
    );

    @Operation(summary = "편지 운영 현황 조회")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "편지 운영 현황 조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 날짜 범위", content = @Content),
            @ApiResponse(responseCode = "403", description = "관리자 권한 필요", content = @Content)
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/letters/status")
    ResponseEntity<GlobalResponse<LetterStatusRes>> getLetterStatus(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    );

    @Operation(summary = "나쁜말 AI 차단 현황 조회",
            description = "type: LETTER / REPLY / PRAISE / ALL (기본값 ALL). 차단 기준 80% 이상.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "나쁜말 AI 차단 현황 조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 날짜 범위 또는 type 값", content = @Content),
            @ApiResponse(responseCode = "403", description = "관리자 권한 필요", content = @Content)
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/ai/bad-words/status")
    ResponseEntity<GlobalResponse<BadWordStatusRes>> getBadWordStatus(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(description = "차단 유형 필터 (LETTER | REPLY | PRAISE | ALL)")
            @RequestParam(required = false, defaultValue = "ALL") BlockType type
    );
}