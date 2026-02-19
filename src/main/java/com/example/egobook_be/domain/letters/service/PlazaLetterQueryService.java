package com.example.egobook_be.domain.letters.service;

import com.example.egobook_be.domain.letters.dto.response.*;
import com.example.egobook_be.domain.letters.entity.PlazaLetter;
import com.example.egobook_be.domain.letters.entity.PlazaLetterMode;
import com.example.egobook_be.domain.letters.entity.PlazaLetterReply;
import com.example.egobook_be.domain.letters.enums.LettersErrorCode;
import com.example.egobook_be.domain.letters.mapper.PlazaLetterMapper;
import com.example.egobook_be.domain.letters.repository.PlazaLetterReplyReportRepository;
import com.example.egobook_be.domain.letters.repository.PlazaLetterReplyRepository;
import com.example.egobook_be.domain.letters.repository.PlazaLetterRepository;
import com.example.egobook_be.domain.user.entity.User;
import com.example.egobook_be.domain.user.repository.UserRepository;
import com.example.egobook_be.global.exception.CustomException;
import com.example.egobook_be.global.response.SliceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;
import java.util.stream.Collectors;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PlazaLetterQueryService {

    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 50;

    private final PlazaLetterRepository plazaLetterRepository;
    private final PlazaLetterReplyRepository plazaLetterReplyRepository;
    private final PlazaLetterReplyReportRepository replyReportRepository;
    private final PlazaLetterMapper plazaLetterMapper;
    private final UserRepository userRepository;


    @Transactional(readOnly = true)
    public SliceResponse<PlazaSentLetterResDto> getMySentLetters(Long userId, Integer page, Integer size) {

        int safePage = (page == null || page < 1) ? 1 : page;
        int safeSize = (size == null || size <= 0) ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);

        // 1) Slice 조회 (정렬: createdAt desc, letterId desc)
        Pageable pageable = PageRequest.of(
                safePage - 1,
                safeSize,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("letterId"))
        );

        Slice<PlazaLetter> sliceResult = plazaLetterRepository.findMySentLettersSlice(userId, pageable);

        // 2) DTO로 변환하여 반환
        return SliceResponse.of(sliceResult, plazaLetterMapper::toDto);
    }

    @Transactional(readOnly = true)
    public SliceResponse<PlazaReceivedReplyResDto> getRepliesToMyLetters(
            Long userId,
            int page,
            int size
    ) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 50);

        Pageable pageable = PageRequest.of(
                safePage - 1,
                safeSize,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        // 1. 답장 목록 조회
        Slice<PlazaLetterReply> slice =
                plazaLetterReplyRepository.findRepliesForMyLetters(userId, pageable);

        if (slice.isEmpty()) {
            return SliceResponse.of(slice, r -> null);
        }

        // 2. 신고 상태 확인 (내가 신고한 답장 ID 목록 가져옴)
        List<Long> replyIds = slice.getContent().stream()
                .map(PlazaLetterReply::getReplyId)
                .toList();

        java.util.Set<Long> reportedSet = replyIds.isEmpty()
                ? java.util.Collections.emptySet()
                : new java.util.HashSet<>(replyReportRepository.findReportedReplyIds(userId, replyIds));

        // 3) N+1 방지: 답장 작성자(replierId)들 한 번에 조회
        List<Long> replierIds = slice.getContent().stream()
                .map(PlazaLetterReply::getReplierId)
                .distinct()
                .toList();

        java.util.Map<Long, String> replierNicknameMap = replierIds.isEmpty()
                ? java.util.Collections.emptyMap()
                : userRepository.findAllById(replierIds).stream()
                .collect(java.util.stream.Collectors.toMap(
                        User::getId,
                        user -> {
                            String nickname = user.getNickname();
                            return (nickname == null || nickname.isBlank()) ? "친구" : nickname;
                        }
                ));

        // 4) DTO 변환
        return SliceResponse.of(slice, reply -> {
            PlazaLetter letter = reply.getLetter();
            if (letter == null) {
                throw new CustomException(LettersErrorCode.LETTER_NOT_FOUND);
            }

            boolean reported = reportedSet.contains(reply.getReplyId());

            String fromLabel = "익명";
            if (letter.getMode() == PlazaLetterMode.FRIEND) {
                fromLabel = replierNicknameMap.getOrDefault(reply.getReplierId(), "친구");
            }

            return plazaLetterMapper.toReceivedReplyDto(letter, reply, reported, fromLabel);
        });
    }


    @Transactional(readOnly = true)
    public PlazaLetterDetailResDto getMyLetterDetail(Long userId, Long letterId) {

        PlazaLetter letter = plazaLetterRepository.findById(letterId)
                .orElseThrow(() -> new CustomException(LettersErrorCode.LETTER_NOT_FOUND));

        // 내가 보낸 편지인지 검증
        if (!userId.equals(letter.getSenderId())) {
            throw new CustomException(LettersErrorCode.FORBIDDEN);
        }

        // 답장은 없을 수도 있음
        PlazaLetterReply reply =
                plazaLetterReplyRepository.findByLetter(letter).orElse(null);

        boolean reported = false;
        if (reply != null) {
            reported = replyReportRepository
                    .existsByReply_ReplyIdAndReporterId(reply.getReplyId(), userId);
        }

        return plazaLetterMapper.toDetailDto(letter, reply, reported);
    }

    @Transactional(readOnly = true)
    public SliceResponse<DeferredInboxItemDto> getMyDeferredInbox(Long userId, Integer page, Integer size) {

        int safePage = (page == null || page < 1) ? 1 : page;
        int safeSize = (size == null || size <= 0) ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);

        Pageable pageable = PageRequest.of(
                safePage - 1,
                safeSize,
                Sort.by(Sort.Order.desc("arrivedAt"), Sort.Order.desc("letterId"))
        );

        Slice<PlazaLetter> slice = plazaLetterRepository.findMyDeferredInboxSlice(userId, pageable);

        return SliceResponse.of(slice, plazaLetterMapper::toDeferredInboxItemDto);
    }


    @Transactional(readOnly = true)
    public InboxNextResponse getInboxLetterDetail(Long userId, Long letterId) {

        PlazaLetter letter = plazaLetterRepository
                .findInboxLetterForReply(letterId, userId)
                .orElseThrow(() -> new CustomException(LettersErrorCode.LETTER_NOT_FOUND));

        return plazaLetterMapper.toResponse(letter);
    }

}

