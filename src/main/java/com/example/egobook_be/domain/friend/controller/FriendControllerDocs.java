package com.example.egobook_be.domain.friend.controller;

import com.example.egobook_be.domain.friend.dto.*;
import com.example.egobook_be.global.response.GlobalResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Tag(name = "Friend Controller", description = "친구 신청 및 친구 관계 관리 API")
public interface FriendControllerDocs {

    @Operation(
            summary = "친구 신청",
            description = """
                다른 사용자에게 친구 신청을 보냅니다.
                
                - receiverId에는 **상대방의 userId**를 전달해야 합니다.
                - 자기 자신에게는 신청할 수 없습니다.
                - 이미 친구이거나, 이미 신청을 보낸 상태라면 실패합니다.
                """
    )
    ResponseEntity<GlobalResponse<Void>> requestFriend(
            @Parameter(hidden = true)
            @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId,
            @RequestBody @Valid FriendRequestCreateReqDto reqDto
    );

    @Operation(
            summary = "친구 신청 수락",
            description = """
                받은 친구 신청을 수락합니다.
                
                - 수락 시 양방향 친구 관계가 생성됩니다.
                - 친구는 최대 10명까지만 추가할 수 있으며, 초과 시 수락이 실패합니다.
                """
    )
    ResponseEntity<GlobalResponse<Void>> acceptFriend(
            @Parameter(hidden = true)
            @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId,
            @PathVariable Long requestId
    );

    @Operation(
            summary = "친구 신청 거절",
            description = """
                받은 친구 신청을 거절합니다.
                
                - 상태가 REJECTED로 변경됩니다.
                """
    )
    ResponseEntity<GlobalResponse<Void>> rejectFriend(
            @Parameter(hidden = true)
            @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId,
            @PathVariable Long requestId
    );

    @Operation(
            summary = "친구 신청 취소",
            description = """
                내가 보낸 친구 신청을 취소합니다.
                
                - 신청 내역이 삭제됩니다.
                """
    )
    ResponseEntity<GlobalResponse<Void>> cancelFriendRequest(
            @Parameter(hidden = true)
            @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId,
            @PathVariable Long requestId
    );

    @Operation(
            summary = "친구 삭제",
            description = """
                이미 친구인 사용자를 친구 목록에서 삭제합니다.
                
                - 양방향 친구 관계가 모두 제거됩니다.
                """
    )
    ResponseEntity<GlobalResponse<Void>> deleteFriend(
            @Parameter(hidden = true)
            @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId,
            @PathVariable Long friendId
    );

    @Operation(
            summary = "받은 친구 신청 목록 조회",
            description = "내가 받은 친구 신청(PENDING 상태) 목록을 조회합니다."
    )
//    ResponseEntity<GlobalResponse<FriendRequestListWithCountResDto>> getIncomingRequests(
    ResponseEntity<GlobalResponse<List<FriendRequestListResDto>>> getIncomingRequests(

    @Parameter(hidden = true)
            @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId
    );

    @Operation(
            summary = "보낸 친구 신청 목록 조회",
            description = "내가 보낸 친구 신청(PENDING 상태) 목록을 조회합니다."
    )
    ResponseEntity<GlobalResponse<List<FriendRequestListResDto>>> getOutgoingRequests(
            @Parameter(hidden = true)
            @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId
    );

    @Operation(
            summary = "친구 리스트 조회",
            description = """
                로그인한 사용자의 친구 목록을 조회합니다.
                
                - 이미 수락(ACCEPTED)된 친구만 조회됩니다.
                - 친구의 userId와 nickname을 반환합니다.
                """
    )
    ResponseEntity<GlobalResponse<FriendListResDto>> getFriends(
            @Parameter(hidden = true)
            @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId
    );

    @Operation(
            summary = "친구 검색",
            description = """
                닉네임 또는 친구 코드(검색어)를 기반으로 친구를 검색합니다.
                
                - 자기 자신은 검색 결과에서 제외됩니다.
                - 이미 친구이거나, 이미 친구 신청을 보낸 사용자는 검색 결과에서 제외됩니다.
                """
    )
    ResponseEntity<GlobalResponse<List<FriendSearchResDto>>> searchFriends(
            @Parameter(hidden = true)
            @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId,
            @RequestParam String keyword
    );
}