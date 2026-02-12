package com.example.egobook_be.domain.friend.controller;

import com.example.egobook_be.domain.friend.dto.*;
import com.example.egobook_be.domain.friend.service.FriendService;
import com.example.egobook_be.global.response.GlobalResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/friends")
public class FriendController implements FriendControllerDocs {

    private final FriendService friendService;

    @PostMapping("/requests")
    public ResponseEntity<GlobalResponse<Void>> requestFriend(
            @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId,
            @RequestBody @Valid FriendRequestCreateReqDto reqDto
    ) {
        friendService.requestFriend(userId, reqDto);
        return ResponseEntity.ok(GlobalResponse.success("친구 신청 완료", null));
    }

    @PostMapping("/requests/{requestId}/accept")
    public ResponseEntity<GlobalResponse<Void>> acceptFriend(
            @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId,
            @PathVariable Long requestId
    ) {
        friendService.acceptRequest(userId, requestId);
        return ResponseEntity.ok(GlobalResponse.success("친구 신청 수락 완료", null));
    }

    @PostMapping("/requests/{requestId}/reject")
    public ResponseEntity<GlobalResponse<Void>> rejectFriend(
            @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId,
            @PathVariable Long requestId
    ) {
        friendService.rejectRequest(userId, requestId);
        return ResponseEntity.ok(GlobalResponse.success("친구 신청 거절 완료", null));
    }

    @DeleteMapping("/requests/{requestId}")
    public ResponseEntity<GlobalResponse<Void>> cancelFriendRequest(
            @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId,
            @PathVariable Long requestId
    ) {
        friendService.cancelRequest(userId, requestId);
        return ResponseEntity.ok(GlobalResponse.success("친구 신청 취소 완료", null));
    }

    @DeleteMapping("/{friendId}")
    public ResponseEntity<GlobalResponse<Void>> deleteFriend(
            @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId,
            @PathVariable Long friendId
    ) {
        friendService.deleteFriend(userId, friendId);
        return ResponseEntity.ok(GlobalResponse.success("친구 삭제 완료", null));
    }

//    @GetMapping("/requests/incoming")
//    public ResponseEntity<GlobalResponse<FriendRequestListWithCountResDto>> getIncomingRequests(
//            @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId
//    ) {
//        return ResponseEntity.ok(
//                GlobalResponse.success("받은 친구 신청 목록 조회 성공", friendService.getIncomingRequests(userId))
//        );
//    }

    @GetMapping("/requests/incoming")
    public ResponseEntity<GlobalResponse<List<FriendRequestListResDto>>> getIncomingRequests(
            @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId
    ) {
        return ResponseEntity.ok(
                GlobalResponse.success(
                        "받은 친구 신청 목록 조회 성공",
                        friendService.getIncomingRequests(userId)
                )
        );
    }


    @GetMapping("/requests/outgoing")
    public ResponseEntity<GlobalResponse<List<FriendRequestListResDto>>> getOutgoingRequests(
            @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId
    ) {
        return ResponseEntity.ok(
                GlobalResponse.success("보낸 친구 신청 목록 조회 성공", friendService.getOutgoingRequests(userId))
        );
    }

    @GetMapping
    public ResponseEntity<GlobalResponse<FriendListResDto>> getFriends(
            @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId
    ) {
        return ResponseEntity.ok(
                GlobalResponse.success("친구 리스트 조회 성공", friendService.getFriends(userId))
        );
    }

    @GetMapping("/search")
    public ResponseEntity<GlobalResponse<List<FriendSearchResDto>>> searchFriends(
            @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId,
            @RequestParam String keyword
    ) {
        return ResponseEntity.ok(
                GlobalResponse.success("친구 검색 성공", friendService.searchFriends(userId, keyword))
        );
    }
}
