package com.example.egobook_be.plaza.letters.controller;

import com.example.egobook_be.global.response.GlobalResponse;
import com.example.egobook_be.plaza.letters.dto.InboxNextResponse;
import com.example.egobook_be.plaza.letters.service.PlazaLetterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/plaza/letters")
public class PlazaLetterController {

    private final PlazaLetterService plazaLetterService;

    @Operation(
            summary = "내가 답장해야 할 '도착 편지' 1건 가져오기",
            description = "광장 첫 진입 시 편지 도착 팝업을 띄우기 위한 용도. 도착한 편지가 없으면 letter=null"
    )
    @ApiResponse(
            responseCode = "200",
            description = "성공",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = InboxNextResponse.class),
                    examples = @ExampleObject(
                            name = "example",
                            value =
                                    "{\n" +
                                            "  \"letter\": {\n" +
                                            "    \"letterId\": 301,\n" +
                                            "    \"status\": \"ARRIVED\",\n" +
                                            "    \"mode\": \"RANDOM\",\n" +
                                            "    \"fromLabel\": \"익명\",\n" +
                                            "    \"content\": \"요즘 너무 지치는데… 어떻게 버티지?\",\n" +
                                            "    \"arrivedAt\": \"2026-01-04T10:00:00+09:00\",\n" +
                                            "    \"replyDeadlineAt\": \"2026-01-05T10:00:00+09:00\"\n" +
                                            "  }\n" +
                                            "}"
                    )
            )
    )
    @GetMapping("/inbox/next")
    public GlobalResponse<InboxNextResponse> getInboxNext(
            @Parameter(description = "테스트용 유저 ID(초기에는 헤더로 받음)", example = "1")
            @RequestHeader(name = "X-USER-ID", required = false) Long userId
    ) {
        // 초기 테스트 편의: 헤더 없으면 1로 고정
        Long resolvedUserId = (userId == null) ? 1L : userId;

        InboxNextResponse result = plazaLetterService.getNextArrivedLetter(resolvedUserId);

        // 너 프로젝트 GlobalResponse 팩토리 메서드 이름이 다르면 여기만 맞춰줘
        return GlobalResponse.success(result);
    }
}

