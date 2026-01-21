package com.example.egobook_be.domain.letters.service;

import com.example.egobook_be.domain.letters.entity.PlazaLetter;
import com.example.egobook_be.domain.letters.entity.PlazaLetterStatus;
import com.example.egobook_be.domain.letters.dto.response.PlazaSentLetterResDto;
import com.example.egobook_be.domain.letters.repository.PlazaLetterRepository;
import com.example.egobook_be.global.response.SliceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class PlazaLetterQueryService {

    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 50;

    private static final String AI_FROM_LABEL = "당신의 구독";
    private static final String AI_PREVIEW = "48시간 동안 답장이 없어 내가 대신...";

    private final PlazaLetterRepository plazaLetterRepository;

    @Transactional
    public SliceResponse<PlazaSentLetterResDto> getMySentLetters(Long userId, Integer slice, Integer size) {

        int safeSlice = (slice == null || slice < 1) ? 1 : slice; // 프론트 기준 1부터
        int safeSize = (size == null || size <= 0) ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);

        // 1) 48시간 지난 SENT -> AI_REPLIED 처리
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime threshold = now.minusHours(48);

        plazaLetterRepository.bulkMarkAiReplied(
                userId,
                threshold,
                PlazaLetterStatus.AI_REPLIED,
                AI_FROM_LABEL
        );

        // 2) Slice 조회 (정렬: createdAt desc, letterId desc)
        Pageable pageable = PageRequest.of(
                safeSlice - 1,
                safeSize,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("letterId"))
        );

        Slice<PlazaLetter> sliceResult = plazaLetterRepository.findMySentLettersSlice(userId, pageable);

        // 3) SliceResponse로 변환 (Entity -> Dto 매핑 포함)
        return SliceResponse.of(sliceResult, this::toDto);
    }

    private PlazaSentLetterResDto toDto(PlazaLetter letter) {

        OffsetDateTime createdAt = letter.getCreatedAt();
        OffsetDateTime aiReplaceAt = (createdAt == null) ? null : createdAt.plusHours(48);

        String preview = (letter.getStatus() == PlazaLetterStatus.AI_REPLIED)
                ? AI_PREVIEW
                : truncate(letter.getContent(), 30);

        return PlazaSentLetterResDto.builder()
                .letterId(letter.getLetterId())
                .mode(letter.getMode())
                .status(letter.getStatus())
                .aiReplaceAt(aiReplaceAt)
                .lastMessagePreview(preview)
                .createdAt(createdAt)
                .build();
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        if (s.length() <= max) return s;
        return s.substring(0, max) + "...";
    }
}
