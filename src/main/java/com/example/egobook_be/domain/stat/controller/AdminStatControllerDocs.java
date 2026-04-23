package com.example.egobook_be.domain.stat.controller;

import com.example.egobook_be.domain.stat.dto.*;
import com.example.egobook_be.domain.stat.enums.AdminStatUnit;
import com.example.egobook_be.global.response.GlobalResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

public interface AdminStatControllerDocs {

    @Operation(summary = "DAU/MAU 통계 조회", description = """
            선택한 날짜 범위에 DAU/MAU 통계를 조회합니다.
            
            - 선택한 날짜 범위는 90일 이하여야 합니다.
            - dau : 해당 날짜 활성 사용자 수
            - mau : 해당 날짜 기준 최근 30일 활성 사용자 수
            """)
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/users/dau-mau")
    ResponseEntity<GlobalResponse<AdminDauMauResDto>> getDauMau(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    );

    @Operation(summary = "신규 가입/탈퇴 통계 조회", description = """
            선택한 기간/단위 별 신규 가입/탈퇴 수와 증감을 조회합니다.
            
            - 선택 단위 : 주(WEEK)/월(MONTH)
            - period : 집계 기간 (iso 기준 주차: 2026-W07 / 월: 2026-01)
            """)
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/users/join-withdraw")
    ResponseEntity<GlobalResponse<AdminJoinWithdrawResDto>> getJoinWithdraw(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "MONTH") AdminStatUnit unit
    );

    @Operation(summary = "잔존율 조회", description = """
            7/30일 잔존율을 조회합니다.
            
            - 7일 잔존율 : 전체 가입 유저 중 가입 후 7일째 되는 날 접속한 유저 비율
            - 30일 잔존율 : 전체 가입 유저 중 가입 후 30일째 되는 날 접속한 유저 비율
            """)
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/users/retention")
    ResponseEntity<GlobalResponse<AdminRetentionResDto>> getRetention();

    @Operation(summary = "일기 작성 통계 조회", description = """
            기간/타입별 작성된 일기 수 통계를 조회합니다.
            """)
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/diaries")
    ResponseEntity<GlobalResponse<AdminDiaryStatResDto>> getDiaryTypeStat(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    );

    @Operation(summary = "편지 답장 포기 통계 조회", description = """
            기간별 편지 답장 포기 통계를 조회합니다.
            """)
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/letters/give-up")
    ResponseEntity<GlobalResponse<AdminLetterGiveUpStatResDto>> getLetterGiveUpStat(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    );

    @Operation(summary = "잉크 발행/소비 통계 조회", description = """
            기간/단위 별 잉크 발행/소비 통계를 조회합니다.
            
            - 선택 단위 : 주(WEEK)/월(MONTH)
            - period : 집계 기간 (iso 기준 주차: 2026-W07 / 월: 2026-01)
            """)
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/ink")
    ResponseEntity<GlobalResponse<AdminInkStatResDto>> getInkStat(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "MONTH") AdminStatUnit unit
    );

    @Operation(summary = "탈퇴 사유 통계 조회", description = """
            기간 별 탈퇴 사유 통계를 조회합니다.
            
            - 탈퇴 사유 타입에 따른 수와 비율 반환
            - 기타 사유 텍스트 목록 (reason이 OTHER일 때만 반환, 나머지는 null)
            """)
    @GetMapping("/withdraw-reason")
    ResponseEntity<GlobalResponse<AdminWithdrawReasonResDto>> getWithdrawReason(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    );
}
