package com.example.egobook_be.domain.user.controller;

import com.example.egobook_be.domain.user.dto.SearchUserResDto;
import com.example.egobook_be.domain.user.enums.UserStatus;
import com.example.egobook_be.domain.user.service.AdminUserService;
import com.example.egobook_be.global.response.GlobalResponse;
import com.example.egobook_be.global.response.SliceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
}