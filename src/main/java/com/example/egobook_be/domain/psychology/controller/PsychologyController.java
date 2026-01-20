package com.example.egobook_be.domain.psychology.controller;

import com.example.egobook_be.domain.psychology.dto.*;
import com.example.egobook_be.domain.psychology.service.PsychologyService;
import com.example.egobook_be.global.response.GlobalResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/psychology")
@RequiredArgsConstructor
public class PsychologyController {

    private final PsychologyService psychologyService;

    @GetMapping("/daily")
    public ResponseEntity<GlobalResponse<DailyKnowledgeResDto>> getDaily() {
        return ResponseEntity.ok(GlobalResponse.success(psychologyService.getDailyKnowledge(1L)));
    }

    @GetMapping("/daily/status")
    public ResponseEntity<GlobalResponse<DailyStatusResDto>> getDailyStatus() {
        return ResponseEntity.ok(GlobalResponse.success(psychologyService.getDailyStatus(1L)));
    }

    @PostMapping("/{knowledgeId}/save")
    public ResponseEntity<GlobalResponse<KnowledgeSaveResDto>> save(@PathVariable Long knowledgeId) {
        return ResponseEntity.ok(GlobalResponse.success(psychologyService.saveKnowledge(1L, knowledgeId)));
    }

    @DeleteMapping("/{knowledgeId}/save")
    public ResponseEntity<GlobalResponse<KnowledgeDeleteResDto>> delete(@PathVariable Long knowledgeId) {
        return ResponseEntity.ok(GlobalResponse.success(psychologyService.deleteSavedKnowledge(1L, knowledgeId)));
    }

    @GetMapping("/saved")
    public ResponseEntity<GlobalResponse<SavedKnowledgeListResDto>> getSavedList() {
        return ResponseEntity.ok(GlobalResponse.success(psychologyService.getSavedKnowledgeList(1L)));
    }
}