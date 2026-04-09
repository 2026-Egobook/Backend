package com.example.egobook_be.domain.user.controller;

import com.example.egobook_be.domain.letters.enums.BlockType;
import com.example.egobook_be.domain.user.dto.ResendReqDto;
import com.example.egobook_be.domain.user.dto.AdminContentResDto.*;
import com.example.egobook_be.domain.user.service.AdminContentService;
import com.example.egobook_be.global.response.GlobalResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
public class AdminContentController implements AdminContentControllerDocs {

    private final AdminContentService adminContentService;

    @Override
    public ResponseEntity<GlobalResponse<DailyPraiseStatusRes>> getDailyPraiseStatus(
            LocalDate startDate, LocalDate endDate) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(GlobalResponse.success(200, "일간 칭찬서 발송 현황 조회 성공",
                        adminContentService.getDailyPraiseStatus(startDate, endDate)));
    }

    @Override
    public ResponseEntity<GlobalResponse<ResendRes>> resendDailyPraise(ResendReqDto reqDto) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(GlobalResponse.success(200, "일간 칭찬서 재발송 처리 완료",
                        adminContentService.resendDailyPraise(reqDto)));
    }

    @Override
    public ResponseEntity<GlobalResponse<WeeklyReportStatusRes>> getWeeklyReportStatus(
            LocalDate startDate, LocalDate endDate) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(GlobalResponse.success(200, "주간 리포트 발송 현황 조회 성공",
                        adminContentService.getWeeklyReportStatus(startDate, endDate)));
    }

    @Override
    public ResponseEntity<GlobalResponse<ResendRes>> resendWeeklyReport(ResendReqDto reqDto) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(GlobalResponse.success(200, "주간 리포트 재발송 처리 완료",
                        adminContentService.resendWeeklyReport(reqDto)));
    }

    @Override
    public ResponseEntity<GlobalResponse<LetterStatusRes>> getLetterStatus(
            LocalDate startDate, LocalDate endDate) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(GlobalResponse.success(200, "편지 운영 현황 조회 성공",
                        adminContentService.getLetterStatus(startDate, endDate)));
    }

    @Override
    public ResponseEntity<GlobalResponse<BadWordStatusRes>> getBadWordStatus(
            LocalDate startDate, LocalDate endDate, BlockType type) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(GlobalResponse.success(200, "나쁜말 AI 차단 현황 조회 성공",
                        adminContentService.getBadWordStatus(startDate, endDate, type)));
    }
}