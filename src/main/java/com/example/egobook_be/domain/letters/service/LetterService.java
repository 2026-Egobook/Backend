package com.example.egobook_be.domain.letters.service;

import com.example.egobook_be.domain.letters.entity.PlazaLetter;
import com.example.egobook_be.domain.letters.repository.PlazaLetterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class LetterService {

    private final PlazaLetterRepository plazaLetterRepository;

    // 특정 편지 내용 가져오기
    public String getLetterContentById(Long letterId) {
        PlazaLetter letter = plazaLetterRepository.findById(letterId)
                .orElseThrow(() -> new IllegalArgumentException("편지를 찾을 수 없습니다."));
        return letter.getContent();
    }

    // AI 답장이 필요한지 여부를 확인
    public boolean isEligibleForAIReply(Long letterId) {
        PlazaLetter letter = plazaLetterRepository.findById(letterId)
                .orElseThrow(() -> new IllegalArgumentException("편지를 찾을 수 없습니다."));

        OffsetDateTime arrivedAt = letter.getArrivedAt();
        if (arrivedAt == null) {
            return false; // 편지가 도착하지 않았으면 AI 답장이 불필요
        }

        // 48시간이 경과했는지 확인
        return arrivedAt.plus(48, ChronoUnit.HOURS).isBefore(OffsetDateTime.now());
    }
}
