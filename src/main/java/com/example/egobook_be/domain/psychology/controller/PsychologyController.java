package com.example.egobook_be.domain.psychology.controller;

import com.example.egobook_be.domain.psychology.dto.*;
import com.example.egobook_be.domain.psychology.service.PsychologyService;
import com.example.egobook_be.global.response.GlobalResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "심리 지식 API")
@RestController
@RequestMapping("/psychology")
@RequiredArgsConstructor
public class PsychologyController {
    private final PsychologyService psychologyService;

    @Operation(
            summary = "오늘의 심리 지식 조회",
            description = "사용자에게 매일 새로운 심리 지식을 제공하며, 최초 조회 시 잉크 보상을 지급합니다."
    )
    @GetMapping("/daily")
    public ResponseEntity<GlobalResponse<DailyKnowledgeResDto>> getDaily(
            @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId
    ) {
        return ResponseEntity.ok(GlobalResponse.success(psychologyService.getDailyKnowledge(userId)));
    }

    @Operation(
            summary = "화면 물병 표시 여부 조회",
            description = "사용자가 당일의 심리 지식을 조회하면 false, 조회하지 않았으면 true를 반환합니다."
    )
    @GetMapping("/daily/status")
    public ResponseEntity<GlobalResponse<DailyStatusResDto>> getDailyStatus(
            @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId
    ) {
        return ResponseEntity.ok(GlobalResponse.success(psychologyService.getDailyStatus(userId)));
    }

    @Operation(
            summary = "심리 지식 북마크 저장",
            description = "심리 지식 북마크에 저장합니다."
    )
    @PostMapping("/{knowledgeId}/save")
    public ResponseEntity<GlobalResponse<KnowledgeSaveResDto>> save(
            @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId,
            @PathVariable Long knowledgeId) {
        return ResponseEntity.ok(GlobalResponse.success(psychologyService.saveKnowledge(userId, knowledgeId)));
    }

    @Operation(
            summary = "심리 지식 북마크 저장 취소",
            description = "심리 지식 북마크에 저장한것을 취소합니다. "
    )
    @DeleteMapping("/{knowledgeId}/save")
    public ResponseEntity<GlobalResponse<KnowledgeDeleteResDto>> delete(
            @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId,
            @PathVariable Long knowledgeId) {
        return ResponseEntity.ok(GlobalResponse.success(psychologyService.deleteSavedKnowledge(userId, knowledgeId)));
    }

    @Operation(
            summary = "심리 지식 북마크 조회",
            description = "북마크한 심리 지식을 조회합니다."
    )
    @GetMapping("/saved")
    public ResponseEntity<GlobalResponse<SavedKnowledgeListResDto>> getSavedList(
            @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId
    ) {
        return ResponseEntity.ok(GlobalResponse.success(psychologyService.getSavedKnowledgeList(userId)));
    }
}