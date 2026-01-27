package com.example.egobook_be.domain.home.controller;

import com.example.egobook_be.domain.home.dto.HomeResDto;
import com.example.egobook_be.domain.home.service.HomeService;
import com.example.egobook_be.global.response.GlobalResponse;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class HomeController implements HomeControllerDocs{
    private final HomeService homeService;

    /**
     * [Home 화면의 데이터들 반환]
     * GET /home
     */
    @Override
    public ResponseEntity<GlobalResponse<HomeResDto>> getHomeData(
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId
    ){
        HomeResDto resDto = homeService.getHomeData(userId);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(GlobalResponse.success("Home 화면 데이터 조회 성공", resDto));
    }
}
