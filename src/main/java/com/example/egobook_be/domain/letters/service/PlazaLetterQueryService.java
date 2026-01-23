package com.example.egobook_be.domain.letters.service;

import com.example.egobook_be.domain.letters.entity.PlazaLetter;
import com.example.egobook_be.domain.letters.entity.PlazaLetterStatus;
import com.example.egobook_be.domain.letters.dto.response.PlazaSentLetterResDto;
import com.example.egobook_be.domain.letters.mapper.PlazaLetterMapper;
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

    private final PlazaLetterRepository plazaLetterRepository;
    private final PlazaLetterMapper plazaLetterMapper;

    @Transactional(readOnly = true)
    public SliceResponse<PlazaSentLetterResDto> getMySentLetters(Long userId, Integer slice, Integer size) {

        int safeSlice = (slice == null || slice < 1) ? 1 : slice; // 프론트 기준 1부터
        int safeSize = (size == null || size <= 0) ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);

        // 1) Slice 조회 (정렬: createdAt desc, letterId desc)
        Pageable pageable = PageRequest.of(
                safeSlice - 1,
                safeSize,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("letterId"))
        );

        Slice<PlazaLetter> sliceResult = plazaLetterRepository.findMySentLettersSlice(userId, pageable);

        // 2) DTO로 변환하여 반환
        return SliceResponse.of(sliceResult, plazaLetterMapper::toDto);
    }
}

