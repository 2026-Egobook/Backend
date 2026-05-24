package com.example.egobook_be.domain.letters.service;

import com.example.egobook_be.domain.letters.entity.BadWordBlockLog;
import com.example.egobook_be.domain.letters.enums.BlockType;
import com.example.egobook_be.domain.letters.repository.BadWordBlockLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BadWordBlockLogService {

    private final BadWordBlockLogRepository badWordBlockLogRepo;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveBlockLog(Long userId, BlockType type, String text,
                             List<String> badWords, double score) {
        badWordBlockLogRepo.save(BadWordBlockLog.builder()
                .userId(userId)
                .type(type)
                .originalText(text)
                .badWords(badWords != null ? badWords : List.of())
                .score(score)
                .blockedAt(LocalDateTime.now())
                .build());
    }
}