package com.example.egobook_be.domain.friend.service;

import com.example.egobook_be.domain.friend.dto.FriendRequestCreateReqDto;
import com.example.egobook_be.domain.friend.dto.FriendRequestListResDto;
import com.example.egobook_be.domain.friend.dto.FriendResDto;
import com.example.egobook_be.domain.friend.dto.FriendSearchResDto;
import com.example.egobook_be.domain.friend.entity.Friend;
import com.example.egobook_be.domain.friend.entity.FriendRequest;
import com.example.egobook_be.domain.friend.enums.FriendRequestStatus;
import com.example.egobook_be.domain.friend.exception.FriendErrorCode;
import com.example.egobook_be.domain.friend.repository.FriendRepository;
import com.example.egobook_be.domain.friend.repository.FriendRequestRepository;
import com.example.egobook_be.domain.user.entity.User;
import com.example.egobook_be.domain.user.repository.UserRepository;
import com.example.egobook_be.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FriendService {

    private final FriendRequestRepository friendRequestRepository;
    private final FriendRepository friendRepository;
    private final UserRepository userRepository;

    /** 친구 신청 **/
    @Transactional
    public void requestFriend(Long senderId, FriendRequestCreateReqDto reqDto) {

        if (senderId.equals(reqDto.receiverId())) {
            throw new CustomException(FriendErrorCode.SELF_REQUEST_NOT_ALLOWED);
        }

        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new CustomException(FriendErrorCode.USER_NOT_FOUND));

        User receiver = userRepository.findById(reqDto.receiverId())
                .orElseThrow(() -> new CustomException(FriendErrorCode.USER_NOT_FOUND));

        // 이미 친구면 다시 신청 못하도록
        if (friendRepository.existsByUserAndFriend(sender, receiver)) {
            throw new CustomException(FriendErrorCode.ALREADY_FRIEND);
        }

        // 기존 신청 이력 확인
        friendRequestRepository
                .findBySenderAndReceiver(sender, receiver)
                .ifPresent(existing -> {
                    if (existing.getStatus() == FriendRequestStatus.PENDING) {
                        throw new CustomException(FriendErrorCode.ALREADY_REQUESTED);
                    }

                    if (existing.getStatus() == FriendRequestStatus.ACCEPTED) {
                        throw new CustomException(FriendErrorCode.ALREADY_FRIEND);
                    }

                    if (existing.getStatus() == FriendRequestStatus.REJECTED) {
                        // 거절된 경우에는 재신청 가능하도록
                        existing.reRequest();
                    }
                });

        // 기존 신청이 없을 때만 새로 생성
        if (!friendRequestRepository.findBySenderAndReceiver(sender, receiver).isPresent()) {
            friendRequestRepository.save(
                    FriendRequest.builder()
                            .sender(sender)
                            .receiver(receiver)
                            .status(FriendRequestStatus.PENDING)
                            .build()
            );
        }
    }

    /** 친구 신청 수락 **/
    @Transactional
    public void acceptRequest(Long receiverId, Long requestId) {

        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new CustomException(FriendErrorCode.USER_NOT_FOUND));

        FriendRequest request = friendRequestRepository
                .findByIdAndReceiver(requestId, receiver)
                .orElseThrow(() -> new CustomException(FriendErrorCode.FRIEND_REQUEST_NOT_FOUND));

        request.accept();

        // 양방향 친구 관계 생성
        friendRepository.save(
                Friend.builder()
                        .user(receiver)
                        .friend(request.getSender())
                        .build()
        );
        friendRepository.save(
                Friend.builder()
                        .user(request.getSender())
                        .friend(receiver)
                        .build()
        );
    }

    /** 친구 신청 거절 **/
    @Transactional
    public void rejectRequest(Long receiverId, Long requestId) {

        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new CustomException(FriendErrorCode.USER_NOT_FOUND));

        FriendRequest request = friendRequestRepository
                .findByIdAndReceiver(requestId, receiver)
                .orElseThrow(() -> new CustomException(FriendErrorCode.FRIEND_REQUEST_NOT_FOUND));

        request.reject();
    }

    /** 친구 신청 취소 **/
    @Transactional
    public void cancelRequest(Long senderId, Long requestId) {

        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new CustomException(FriendErrorCode.USER_NOT_FOUND));

        FriendRequest request = friendRequestRepository
                .findByIdAndSender(requestId, sender)
                .orElseThrow(() -> new CustomException(FriendErrorCode.FRIEND_REQUEST_NOT_FOUND));

        friendRequestRepository.delete(request);
    }

    /** 친구 삭제 **/
    @Transactional
    public void deleteFriend(Long userId, Long friendId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(FriendErrorCode.USER_NOT_FOUND));

        User friend = userRepository.findById(friendId)
                .orElseThrow(() -> new CustomException(FriendErrorCode.USER_NOT_FOUND));

        friendRepository.deleteByUserAndFriend(user, friend);
        friendRepository.deleteByUserAndFriend(friend, user);
    }


    /** 내가 받은 친구 신청 목록 (승인 대기) **/
    @Transactional(readOnly = true)
    public List<FriendRequestListResDto> getIncomingRequests(Long userId) {

        User receiver = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(FriendErrorCode.USER_NOT_FOUND));

        return friendRequestRepository
                .findByReceiverAndStatus(receiver, FriendRequestStatus.PENDING)
                .stream()
                .map(req -> FriendRequestListResDto.builder()
                        .requestId(req.getId())
                        .userId(req.getSender().getId())
                        .nickname(req.getSender().getNickname())
                        .requestedAt(req.getCreatedAt())
                        .build()
                )
                .toList();
    }

    /** 내가 보낸 친구 신청 목록 **/
    @Transactional(readOnly = true)
    public List<FriendRequestListResDto> getOutgoingRequests(Long userId) {

        User sender = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(FriendErrorCode.USER_NOT_FOUND));

        return friendRequestRepository
                .findBySenderAndStatus(sender, FriendRequestStatus.PENDING)
                .stream()
                .map(req -> FriendRequestListResDto.builder()
                        .requestId(req.getId())
                        .userId(req.getReceiver().getId())
                        .nickname(req.getReceiver().getNickname())
                        .requestedAt(req.getCreatedAt())
                        .build()
                )
                .toList();
    }

    /** 친구 리스트 **/
    @Transactional(readOnly = true)
    public List<FriendResDto> getFriends(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(FriendErrorCode.USER_NOT_FOUND));

        return friendRepository.findByUser(user)
                .stream()
                .map(friend -> FriendResDto.builder()
                        .friendId(friend.getFriend().getId())
                        .nickname(friend.getFriend().getNickname())
                        .build()
                )
                .toList();
    }

    /** 친구 검색 **/
    @Transactional(readOnly = true)
    public List<FriendSearchResDto> searchFriends(Long userId, String keyword) {

        User me = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(FriendErrorCode.USER_NOT_FOUND));

        return userRepository
                .findByNicknameContainingIgnoreCaseOrAccountCodeContainingIgnoreCase(keyword, keyword)
                .stream()
                // 자기 자신 제외
                .filter(user -> !user.getId().equals(userId))
                // 이미 친구인 경우 제외
                .filter(user -> !friendRepository.existsByUserAndFriend(me, user))
                .map(user -> FriendSearchResDto.builder()
                        .userId(user.getId())
                        .nickname(user.getNickname())
                        .level(user.getLevel())
                        .build()
                )
                .toList();
    }
}