package com.example.egobook_be.domain.restriction.controller;

import com.example.egobook_be.domain.restriction.dto.RestrictionCreateReqDto;
import com.example.egobook_be.domain.restriction.dto.RestrictionCreateResDto;
import com.example.egobook_be.domain.restriction.dto.RestrictionItemResDto;
import com.example.egobook_be.domain.restriction.enums.RestrictionStatus;
import com.example.egobook_be.domain.restriction.service.AdminRestrictionService;
import com.example.egobook_be.global.response.GlobalResponse;
import com.example.egobook_be.global.response.SliceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AdminRestrictionController implements AdminRestrictionControllerDocs {

    private final AdminRestrictionService adminRestrictionService;

    @Override
    public ResponseEntity<GlobalResponse<RestrictionCreateResDto>> createRestriction(
            Long adminId,
            Long userId,
            RestrictionCreateReqDto reqDto
    ) {
        RestrictionCreateResDto response = adminRestrictionService.createRestriction(adminId, userId, reqDto);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(GlobalResponse.success(201, "사용자 제재 적용 성공", response));
    }

    // [AI-GEN] 사용자 제재 기록 목록 조회
    @Override
    public ResponseEntity<GlobalResponse<SliceResponse<RestrictionItemResDto>>> getRestrictionList(
            Long userId, int page, int size, RestrictionStatus status) {
        SliceResponse<RestrictionItemResDto> response =
                adminRestrictionService.getRestrictionList(userId, page, size, status);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(GlobalResponse.success(200, "사용자 제재 기록 목록 조회 성공", response));
    }
}
