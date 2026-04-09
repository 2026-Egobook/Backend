package com.example.egobook_be.domain.user.controller;

import com.example.egobook_be.domain.user.dto.AdminUserInfoResDto;
import com.example.egobook_be.domain.user.dto.AdminUserReportHistoryResDto;
import com.example.egobook_be.domain.user.dto.AdminUserStatsResDto;
import com.example.egobook_be.domain.user.dto.SearchUserResDto;
import com.example.egobook_be.domain.user.enums.UserStatus;
import com.example.egobook_be.domain.user.service.AdminUserService;
import com.example.egobook_be.global.enums.ReportDomainType;
import com.example.egobook_be.global.enums.ReportReason;
import com.example.egobook_be.global.enums.ReportStatus;
import com.example.egobook_be.global.enums.ReportType;
import com.example.egobook_be.global.response.GlobalResponse;
import com.example.egobook_be.global.response.SliceResponse;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AdminUserController implements AdminUserControllerDocs {

    private final AdminUserService adminUserService;

    @Override
    public ResponseEntity<GlobalResponse<SliceResponse<SearchUserResDto>>> searchUserList(
            String keyword,
            UserStatus status,
            Integer page,
            Integer size
    ) {
        SliceResponse<SearchUserResDto> response = adminUserService.searchUserList(keyword, status, page, size);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(GlobalResponse.success(200, "회원 리스트 검색 성공", response));
    }

    @Override
    public ResponseEntity<GlobalResponse<AdminUserInfoResDto>> getUserInfo(Long userId) {
        AdminUserInfoResDto response = adminUserService.getUserInfo(userId);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(GlobalResponse.success(200, "회원 기본 정보 조회 성공", response));
    }

    @Override
    public ResponseEntity<GlobalResponse<AdminUserStatsResDto>> getUserStats(Long userId) {
        AdminUserStatsResDto response = adminUserService.getUserStats(userId);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(GlobalResponse.success(200, "회원 활동 통계 조회 성공", response));
    }

    @Override
    public ResponseEntity<GlobalResponse<AdminUserReportHistoryResDto>> getUserReportHistory(
            Long userId,
            ReportDomainType reportDomainType,
            ReportType reportType,
            ReportReason reportReason,
            ReportStatus reportStatus,
            Integer page,
            Integer size
    ) {
        AdminUserReportHistoryResDto response = adminUserService.getUserReportHistory(
                userId, reportDomainType, reportType, reportReason, reportStatus, page, size
        );
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(GlobalResponse.success(200, "사용자 신고 이력 조회 성공", response));
    }
}