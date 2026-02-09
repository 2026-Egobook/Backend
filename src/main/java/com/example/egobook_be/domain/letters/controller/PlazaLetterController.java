package com.example.egobook_be.domain.letters.controller;

import com.example.egobook_be.domain.letters.dto.*;
import com.example.egobook_be.domain.letters.dto.request.CreateLetterRequest;
import com.example.egobook_be.domain.letters.dto.request.ReplyReportRequest;
import com.example.egobook_be.domain.letters.dto.request.ReplyRequest;
import com.example.egobook_be.domain.letters.dto.response.*;
import com.example.egobook_be.domain.letters.entity.ReplyReportReason;
import com.example.egobook_be.domain.letters.service.*;
import com.example.egobook_be.global.response.GlobalResponse;
import com.example.egobook_be.global.response.SliceResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.*;
import com.example.egobook_be.domain.letters.dto.request.LetterReportRequest;
import com.example.egobook_be.domain.letters.service.LetterReportService;


import com.example.egobook_be.domain.letters.service.ai.GeminiClient;

@Tag(name = "Plaza Letter Controller", description = "광장 편지 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/plaza/letters")
public class PlazaLetterController {

    private final PlazaLetterService plazaLetterService;
    private final PlazaLetterQueryService plazaLetterQueryService;
    private final ReplyReportService replyReportService;
    private final LetterReportService letterReportService;


    private final GeminiClient geminiClient;
    private final LetterService letterService;

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
            summary = "내가 작성한 답장 목록 조회",
            description = """
            로그인한 사용자가 작성한 답장을 최신순으로 Slice 조회합니다.
            - page는 1부터 시작합니다.
            - size는 1~50 범위로 제한합니다.
            """
    )
    @GetMapping("/replies")
    public GlobalResponse<SliceResponse<ReplyItemDto>> getMyReplies(
            @Parameter(hidden = true)
            @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        SliceResponse<ReplyItemDto> result = plazaLetterService.getMyReplies(userId, page, size);
        return GlobalResponse.success(result);
    }

    @Operation(
            summary = "편지 작성",
            description = """
            광장 편지를 작성해 전송합니다.
            - text: 360자 이하
            - 하루 1회 제한
            - AI 검사 실패 시 거절
            - RANDOM/FRIEND 모드가 있음 (특정 친구한테 보낼수도있고 랜덤도 가능)
            """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "400", description = "글자수 초과/하루 1회 초과/AI 검사 실패/모드 오류"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public GlobalResponse<CreateLetterResponse> createLetter(
            @Parameter(hidden = true)
            @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId,
            @Valid @RequestBody CreateLetterRequest request
    ) {
        return GlobalResponse.success(plazaLetterService.createLetter(userId, request));
    }

    @Operation(
            summary = "답장 스레드 삭제(내 편지 + 답장 함께 삭제)",
            description = """
            threadId 기준으로 스레드에 속한 편지와 답장을 함께 삭제합니다.
            - senderId 또는 receiverId가 본인인 경우에만 삭제 가능
            """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "스레드 없음")
    })
    @DeleteMapping("/threads/{threadId}")
    public GlobalResponse<DeleteThreadResponse> deleteThread(
            @Parameter(hidden = true)
            @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId,
            @PathVariable Long threadId
    ) {
        return GlobalResponse.success(plazaLetterService.deleteThread(userId, threadId));
    }

    @Operation(
            summary = "내가 보낸 편지 상태 조회(48시간 AI 대체 확인 포함)",
            description = """
        로그인한 사용자가 보낸 편지를 최신순으로 Slice 조회합니다.
        - page는 1부터 시작
        - size는 1~50 권장
        """
    )
    @GetMapping("/sent")
    public GlobalResponse<SliceResponse<PlazaSentLetterResDto>> getMySentLetters(
            @Parameter(hidden = true)
            @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        SliceResponse<PlazaSentLetterResDto> result =
                plazaLetterQueryService.getMySentLetters(userId, page, size);

        return GlobalResponse.success(result);
    }


    @Operation(
            summary = "편지 신고",
            description = """
            사용자가 받은 편지에 대해 신고하는 기능입니다.
            - 신고 사유는 선택지 또는 자유 작성으로 입력할 수 있습니다.
            - '기타(OTHER)' 사유 선택 시 description을 입력해야 합니다.
            - 동일 사용자는 동일 편지를 중복 신고할 수 없습니다.
            - 신고 후 상태는 PENDING으로 저장됩니다.
            - 신고 누적 3회 이상이면 편지가 삭제될 수 있습니다(정책).
        """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 신고 사유(OTHER인데 description 없음 등)"),
            @ApiResponse(responseCode = "403", description = "권한 없음(내가 받은 편지가 아님)"),
            @ApiResponse(responseCode = "404", description = "편지 없음"),
            @ApiResponse(responseCode = "409", description = "이미 신고한 편지")
    })
    @PostMapping("/{letterId}/report")
    public GlobalResponse<String> reportLetter(
            @Parameter(hidden = true)
            @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId,
            @PathVariable Long letterId,
            @RequestBody LetterReportRequest request
    ) {
        letterReportService.reportLetter(
                userId,
                letterId,
                request.getReason(),
                request.getDescription()
        );
        return GlobalResponse.success("신고가 완료되었습니다.");
    }



    @Operation(
            summary = "답장 신고",
            description = """
                사용자가 작성한 답장에 대해 신고하는 기능입니다.
                - 신고 사유는 선택지 또는 자유 작성으로 입력할 수 있습니다.
                - '기타' 사유를 선택한 경우, 추가적인 설명을 자유롭게 작성해야 합니다.
                - 이미 신고된 답장은 다시 신고할 수 없습니다.
                - 신고 후, 신고 상태는 'PENDING'으로 저장됩니다. 관리자가 이를 처리하면 'RESOLVED' 상태로 변경됩니다.
                - 신고된 답장은 내용 수정이나 삭제가 불가능할 수 있습니다.
            """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 신고 사유"),
            @ApiResponse(responseCode = "404", description = "답장 없음"),
            @ApiResponse(responseCode = "409", description = "이미 신고한 답장")
    })
    @PostMapping("replies/{replyId}/report")
    public GlobalResponse<String> reportReply(
            @Parameter(hidden = true)
            @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId,
            @PathVariable Long replyId,
            @RequestBody ReplyReportRequest request
    ) {
        replyReportService.reportReply(
                userId,
                replyId,
                request.getReason(),
                request.getDescription()
        );
        return GlobalResponse.success("신고가 완료되었습니다.");
    }

    @Operation(
            summary = "내가 보낸 편지에 달린 답장 조회",
            description = "상대가 내 편지에 작성한 답장을 최신순으로 조회합니다."
    )
    @GetMapping("/replies/received")
    public GlobalResponse<SliceResponse<PlazaReceivedReplyResDto>> getRepliesToMyLetters(
            @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return GlobalResponse.success(
                plazaLetterQueryService.getRepliesToMyLetters(userId, page, size)
        );
    }


    @Operation(
            summary = "내가 보낸 편지 + 답장 내용 함께 보기",
            description = """
        내가 보낸 편지 1건과 해당 편지에 달린 답장을 함께 조회합니다.
        - 답장이 없으면 reply는 null
        - AI 답장 / 신고 여부 포함
        """
    )
    @GetMapping("/{letterId}")
    public GlobalResponse<PlazaLetterDetailResDto> getMyLetterDetail(
            @Parameter(hidden = true)
            @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId,
            @PathVariable Long letterId
    ) {
        return GlobalResponse.success(
                plazaLetterQueryService.getMyLetterDetail(userId, letterId)
        );
    }

    // AI 답장 확인용 테스트 API
    @Operation(
            summary = "AI 답장 확인용 테스트 API",
            description = """
  단순test api입니다. 답장 조회하는 로직에서도 확인가능합니다.
        """
    )
    @GetMapping("/ai/reply/{letterId}")
    public String getAIReply(@PathVariable Long letterId) {
        // 편지 가져오기
        String letterContent = letterService.getLetterContentById(letterId);

        // 편지 도달 시간이 48시간 이상 지났고 답장이 없을 경우에만 AI 요청
        boolean isEligibleForReply = letterService.isEligibleForAIReply(letterId);

        if (!isEligibleForReply) {
            return "AI 답장 조건을 충족하지 않습니다.";
        }

        // AI 답장 생성
        String aiReply = geminiClient.generateReply("AI", letterContent);

        return aiReply;
    }


    @Operation(
            summary = "답장을 미루고 있는(DEFERRED) 편지 목록 조회",
            description = """
            메인 화면에서 '답장을 기다리는(미뤄둔)' 편지 목록을 Slice로 조회합니다.
            - receiverId = 로그인 사용자
            - status = DEFERRED
            - page는 1부터 시작
            """
    )
    @GetMapping("/inbox/deferred")
    public GlobalResponse<SliceResponse<DeferredInboxItemDto>> getDeferredInbox(
            @Parameter(hidden = true)
            @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return GlobalResponse.success(
                plazaLetterQueryService.getMyDeferredInbox(userId, page, size)
        );
    }


    @Operation(
            summary = "내가 답장해야 할 편지 상세 조회",
            description = """
            미뤄둔(DEFERRED) 또는 방금 도착한(ARRIVED) 편지 1건을 조회합니다.
            - receiverId가 본인인 경우만 가능
            - 이미 답장한 편지는 조회 불가
        """
    )
    @GetMapping("/inbox/{letterId}")
    public GlobalResponse<InboxNextResponse> getInboxLetterDetail(
            @Parameter(hidden = true)
            @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId,
            @PathVariable Long letterId
    ) {
        return GlobalResponse.success(
                plazaLetterQueryService.getInboxLetterDetail(userId, letterId)
        );
    }


}
