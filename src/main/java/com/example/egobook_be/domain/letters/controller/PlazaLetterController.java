package com.example.egobook_be.domain.letters.controller;

import com.example.egobook_be.domain.letters.dto.*;
import com.example.egobook_be.domain.letters.service.PlazaLetterService;
import com.example.egobook_be.global.response.GlobalResponse;
import com.example.egobook_be.global.response.SliceResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
            @Parameter(hidden = true)
            @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId
    ) {
        InboxNextResponse result = plazaLetterService.getNextArrivedLetter(userId);
        return GlobalResponse.success(result);
    }

    @Operation(
            summary = "편지 답장 작성",
            description = """
                도착한 편지에 답장을 작성합니다.
                - 답장 text는 350자 이하여야 합니다.
                - AI 검사(80% 기준)에 실패하면 요청이 거절됩니다.
                - 24시간이 지나 자동 포기(GAVE_UP) 상태가 된 편지는 답장할 수 없습니다.
                """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "400", description = "350자 초과 또는 AI 검사 실패"),
            @ApiResponse(responseCode = "403", description = "권한 없음(내 편지가 아님)"),
            @ApiResponse(responseCode = "404", description = "편지 없음"),
            @ApiResponse(responseCode = "409", description = "이미 답장/포기됨")
    })
    @PostMapping(value = "/{letterId}/reply", consumes = MediaType.APPLICATION_JSON_VALUE)
    public GlobalResponse<ReplyResponse> reply(
            @Parameter(hidden = true)
            @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId,
            @Parameter(description = "답장할 편지 ID", example = "301")
            @PathVariable Long letterId,
            @Valid @RequestBody ReplyRequest request
    ) {
        ReplyResponse result = plazaLetterService.replyToLetter(userId, letterId, request.getText());
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
            @Parameter(hidden = true)
            @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId,
            @Parameter(description = "대상 편지 ID", example = "301")
            @PathVariable Long letterId
    ) {
        DeferResponse result = plazaLetterService.deferLetter(userId, letterId);
        return GlobalResponse.success(result);
    }

    @Operation(
            summary = "포기하기 처리",
            description = """
                답장하지 않기로 선택한 편지를 포기 상태로 변경합니다.
                - status를 GAVE_UP으로 변경하고 gaveUpAt을 저장합니다.
                - 이미 답장된 편지는 포기할 수 없습니다.
                """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "403", description = "권한 없음(내 편지가 아님)"),
            @ApiResponse(responseCode = "404", description = "편지 없음"),
            @ApiResponse(responseCode = "409", description = "이미 답장/이미 포기됨")
    })
    @PostMapping("/{letterId}/give-up")
    public GlobalResponse<GiveUpResponse> giveUp(
            @Parameter(hidden = true)
            @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId,
            @Parameter(description = "대상 편지 ID", example = "301")
            @PathVariable Long letterId
    ) {
        GiveUpResponse result = plazaLetterService.giveUpLetter(userId, letterId);
        return GlobalResponse.success(result);
    }


    @Operation(
            summary = "내가 작성한 답장 목록 조회 (Slice 무한스크롤)",
            description = """
            로그인한 사용자가 작성한 답장을 최신순으로 Slice 조회합니다.
            - page는 0부터 시작합니다.
            - size는 1~50 범위로 제한합니다.
            """
    )
    @GetMapping("/replies")
    public GlobalResponse<SliceResponse<ReplyItemDto>> getMyReplies(
            @Parameter(hidden = true)
            @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        SliceResponse<ReplyItemDto> result = plazaLetterService.getMyReplies(userId, page, size);
        return GlobalResponse.success(result);
    }


}
