package com.example.egobook_be.domain.psychology.controller;

import com.example.egobook_be.domain.psychology.dto.AdminKnowledgeListResDto;
import com.example.egobook_be.domain.psychology.dto.AdminKnowledgeReqDto;
import com.example.egobook_be.domain.psychology.dto.AdminKnowledgeResDto;
import com.example.egobook_be.domain.psychology.dto.KnowledgeInfoResDto;
import com.example.egobook_be.domain.psychology.service.AdminPsychologyService;
import com.example.egobook_be.domain.psychology.service.PsychologyService;
import com.example.egobook_be.global.response.GlobalResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/psychology")
@Tag(name = "Admin Psychology Controller", description = "관리자 [심리지식] 관련 API")
public class AdminPsychologyController {

    private final AdminPsychologyService psychologyService;
    private final AdminPsychologyService adminPsychologyService;

    @Operation(
            summary = "심리 지식 추가",
            description = "[내용, 출처] 정보를 입력해 심리 지식을 추가합니다."
    )
    @PostMapping
    public GlobalResponse<KnowledgeInfoResDto> createKnowledge(@RequestBody AdminKnowledgeReqDto reqDto) {
        KnowledgeInfoResDto response = psychologyService.createKnowledge(reqDto);
        return GlobalResponse.success("심리지식 추가 성공",response);
    }

    @Operation(
            summary = "심리 지식 목록 조회",
            description = "최근에 등록한 심리지식부터 내림차순으로 한 페이지에 20개씩 심리 지식 목록을 조회합니다. page: 1~"
    )
    @GetMapping
    public GlobalResponse<AdminKnowledgeListResDto> getAllKnowledge(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        AdminKnowledgeListResDto response = psychologyService.getAllKnowledgeList(page, size);
        return GlobalResponse.success("심리지식 목록 조회 성공", response);
    }

    @Operation(
            summary = "심리 지식 단건 조회",
            description = "id값으로 심리 지식 한 건을 상세 조회합니다. 삭제하기 전의 심리 지식은 deleted_at이 null, 삭제 후에는 삭제 시간이 저장됩니다."
    )
    @GetMapping("/{psychologyId}")
    public GlobalResponse<AdminKnowledgeResDto> getKnowledge(@PathVariable Long psychologyId) {
        AdminKnowledgeResDto response = adminPsychologyService.getKnowledge(psychologyId);
        return GlobalResponse.success("심리지식 조회 성공", response);
    }

    @Operation(
            summary = "심리 지식 수정",
            description = "심리 지식을 수정합니다. [내용,출처] 중 하나만 수정하더라도 나머지 요소의 내용도 함께 입력하여 요청을 보내야 합니다."
    )
    @PutMapping("/{psychologyId}")
    public GlobalResponse<AdminKnowledgeResDto> updateKnowledge(@PathVariable Long psychologyId, @RequestBody AdminKnowledgeReqDto reqDto) {
        AdminKnowledgeResDto response = adminPsychologyService.updateKnowledge(psychologyId, reqDto);
        return GlobalResponse.success("심리지식 수정 성공", response);
    }

    @Operation(
            summary = "심리 지식 삭제",
            description = "id값으로 심리 지식을 삭제합니다."
    )
    @DeleteMapping("/{psychologyId}")
    public GlobalResponse<Void> deleteKnowledge(@PathVariable Long psychologyId) {
        adminPsychologyService.deleteKnowledge(psychologyId);
        return GlobalResponse.success("심리지식 삭제 성공", null);
    }
}