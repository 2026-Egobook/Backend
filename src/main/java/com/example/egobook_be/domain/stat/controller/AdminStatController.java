package com.example.egobook_be.domain.stat.controller;

import com.example.egobook_be.domain.stat.dto.*;
import com.example.egobook_be.domain.stat.enums.AdminStatUnit;
import com.example.egobook_be.domain.stat.service.AdminStatService;
import com.example.egobook_be.domain.stat.dto.AdminDauMauResDto;
import com.example.egobook_be.domain.stat.dto.AdminInkStatResDto;
import com.example.egobook_be.domain.stat.dto.AdminJoinWithdrawResDto;
import com.example.egobook_be.global.response.GlobalResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/stats")
@Tag(name = "Admin Stat Controller", description = "통계 관리자 API")
public class AdminStatController implements AdminStatControllerDocs {

    private final AdminStatService adminStatService;

    /**
     * [DAU/MAU 통계 조회]
     * GET /dau-mau
     */
    @Override
    public ResponseEntity<GlobalResponse<AdminDauMauResDto>> getDauMau(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
            ) {
        return ResponseEntity.ok(
                GlobalResponse.success(adminStatService.getDauMau(startDate, endDate))
        );
    }

    /**
     * [신규 가입/탈퇴 통계 조회]
     * GET /users/join-withdraw
     */
    @Override
    public ResponseEntity<GlobalResponse<AdminJoinWithdrawResDto>> getJoinWithdraw(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "MONTH") AdminStatUnit unit
            ) {
        return ResponseEntity.ok(
                GlobalResponse.success(adminStatService.getJoinWithdraw(startDate, endDate, unit))
        );
    }

    /**
     * [잔존율 통계 조회]
     * GET /users/retention
     */
    @Override
    public ResponseEntity<GlobalResponse<AdminRetentionResDto>> getRetention() {
        return ResponseEntity.ok(
                GlobalResponse.success(adminStatService.getRetention())
        );
    }

    /**
     * [일기 작성 통계 조회]
     * GET /diaries
     */
    @Override
    public ResponseEntity<GlobalResponse<AdminDiaryStatResDto>> getDiaryTypeStat(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return ResponseEntity.ok(
                GlobalResponse.success(adminStatService.getDiaryTypeStat(startDate, endDate))
        );
    }

    /**
     * [편지 답장 포기 통계 조회]
     * GET /letters/give-up
     */
    @Override
    public ResponseEntity<GlobalResponse<AdminLetterGiveUpStatResDto>> getLetterGiveUpStat(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return ResponseEntity.ok(
                GlobalResponse.success(adminStatService.getLetterGiveUpStat(startDate, endDate))
        );
    }

    /**
     * [잉크 발행/소비 통계 조회]
     * GET /ink
     */
    @Override
    public ResponseEntity<GlobalResponse<AdminInkStatResDto>> getInkStat(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "MONTH") AdminStatUnit unit
    ) {
        return ResponseEntity.ok(
                GlobalResponse.success(adminStatService.getInkStat(startDate, endDate, unit))
        );
    }

    /**
     * [탈퇴 사유 통계 조회]
     * GET /withdraw-reason
     */
    @Override
    public ResponseEntity<GlobalResponse<AdminWithdrawReasonResDto>> getWithdrawReason(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return ResponseEntity.ok(
                GlobalResponse.success(adminStatService.getWithdrawReason(startDate, endDate))
        );
    }
}
