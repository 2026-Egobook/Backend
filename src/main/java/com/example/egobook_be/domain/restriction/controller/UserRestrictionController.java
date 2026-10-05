package com.example.egobook_be.domain.restriction.controller;

import com.example.egobook_be.domain.restriction.dto.MyRestrictionResDto;
import com.example.egobook_be.domain.restriction.service.UserRestrictionService;
import com.example.egobook_be.global.response.GlobalResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

// 사용자용 제재 조회 컨트롤러
@RestController
@RequiredArgsConstructor
public class UserRestrictionController implements UserRestrictionControllerDocs {

    private final UserRestrictionService userRestrictionService;

    @Override
    public ResponseEntity<GlobalResponse<MyRestrictionResDto>> getMyRestrictions(Long userId) {
        MyRestrictionResDto response = userRestrictionService.getMyRestrictions(userId);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(GlobalResponse.success(200, "내 제재 상태 조회 성공", response));
    }
}
