package com.example.egobook_be.domain.letters.service;

import com.example.egobook_be.domain.letters.dto.response.PlazaReceivedReplyResDto;
import com.example.egobook_be.domain.letters.entity.PlazaLetter;
import com.example.egobook_be.domain.letters.dto.response.PlazaSentLetterResDto;
import com.example.egobook_be.domain.letters.entity.PlazaLetterReply;
import com.example.egobook_be.domain.letters.dto.response.PlazaLetterDetailResDto;
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


        Slice<PlazaLetterReply> slice =
                plazaLetterReplyRepository.findRepliesForMyLetters(userId, pageable);

        if (slice.isEmpty()) {
            return SliceResponse.of(slice, r -> null);
        }

        List<Long> letterIds = slice.getContent().stream()
                .map(PlazaLetterReply::getLetterId)
                .distinct()
                .toList();

        List<PlazaLetter> letters = plazaLetterRepository.findByLetterIdIn(letterIds);

        java.util.Map<Long, PlazaLetter> letterMap = letters.stream()
                .collect(java.util.stream.Collectors.toMap(
                        PlazaLetter::getLetterId,
                        l -> l
                ));

        List<Long> replyIds = slice.getContent().stream()
                .map(PlazaLetterReply::getReplyId)
                .toList();

        java.util.Set<Long> reportedSet = replyIds.isEmpty()
                ? java.util.Collections.emptySet()
                : new java.util.HashSet<>(replyReportRepository.findReportedReplyIds(userId, replyIds));

        return SliceResponse.of(slice, reply -> {
            PlazaLetter letter = letterMap.get(reply.getLetterId());
            if (letter == null) {
                throw new CustomException(LettersErrorCode.LETTER_NOT_FOUND);
            }

            boolean reported = reportedSet.contains(reply.getReplyId());
            return plazaLetterMapper.toReceivedReplyDto(letter, reply, reported);
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
                plazaLetterReplyRepository.findByLetterId(letterId).orElse(null);

        boolean reported = false;
        if (reply != null) {
            reported = replyReportRepository
                    .existsByReply_ReplyIdAndReporterId(reply.getReplyId(), userId);
        }

        return plazaLetterMapper.toDetailDto(letter, reply, reported);
    }


}

