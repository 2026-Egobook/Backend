package com.example.egobook_be.plaza.letters.controller;

import com.example.egobook_be.global.response.GlobalResponse;
import com.example.egobook_be.plaza.letters.dto.DeferResponse;
import com.example.egobook_be.plaza.letters.dto.InboxNextResponse;
import com.example.egobook_be.plaza.letters.service.PlazaLetterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import com.example.egobook_be.plaza.letters.dto.ReplyRequest;
import com.example.egobook_be.plaza.letters.dto.ReplyResponse;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
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
        return GlobalResponse.success(result);
    }

    @PostMapping(value = "/{letterId}/reply", consumes = MediaType.APPLICATION_JSON_VALUE)
    public GlobalResponse<ReplyResponse> reply(
            @Parameter(
                    name = "X-USER-ID",
                    description = "테스트용 유저 ID(초기에는 헤더로 받음). JWT 적용 후 제거 예정",
                    in = ParameterIn.HEADER,
                    required = false,
                    example = "1"
            )
            @RequestHeader(name = "X-USER-ID", required = false) Long userId,

            @Parameter(description = "답장할 편지 ID", example = "301")
            @PathVariable Long letterId,

            @Valid @RequestBody ReplyRequest request
    ) {
        Long resolvedUserId = (userId == null) ? 1L : userId;
        ReplyResponse result = plazaLetterService.replyToLetter(resolvedUserId, letterId, request.getText());
        return GlobalResponse.success(result);
    }


    @Operation(
            summary = "나중에(팝업 닫기) 처리",
            description = """
                    도착 편지 팝업에서 X(닫기)를 눌렀을 때 호출합니다.
                    - 해당 편지 상태를 DEFERRED로 변경합니다.
                    - 이미 답장/포기된 편지는 처리할 수 없습니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "403", description = "권한 없음(내 편지가 아님)"),
            @ApiResponse(responseCode = "404", description = "편지 없음"),
            @ApiResponse(responseCode = "409", description = "이미 답장/포기됨")
    })
    @PostMapping("/{letterId}/defer")
    public GlobalResponse<DeferResponse> defer(
            @Parameter(
                    name = "X-USER-ID",
                    description = "테스트용 유저 ID(초기에는 헤더로 받음). JWT 적용 후 제거 예정",
                    in = ParameterIn.HEADER,
                    required = false,
                    example = "1"
            )
            @RequestHeader(name = "X-USER-ID", required = false) Long userId,
            @Parameter(description = "대상 편지 ID", example = "301")
            @PathVariable Long letterId
    ) {
        Long resolvedUserId = (userId == null) ? 1L : userId;
        DeferResponse result = plazaLetterService.deferLetter(resolvedUserId, letterId);
        return GlobalResponse.success(result);
    }

}

