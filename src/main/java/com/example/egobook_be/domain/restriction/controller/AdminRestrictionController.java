package com.example.egobook_be.domain.restriction.controller;

import com.example.egobook_be.domain.restriction.dto.RestrictionCreateReqDto;
import com.example.egobook_be.domain.restriction.dto.RestrictionCreateResDto;
import com.example.egobook_be.domain.restriction.service.AdminRestrictionService;
import com.example.egobook_be.global.response.GlobalResponse;
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
}
