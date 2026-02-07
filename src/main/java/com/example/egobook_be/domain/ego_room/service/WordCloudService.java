package com.example.egobook_be.domain.ego_room.service;

import com.example.egobook_be.domain.diary.entity.Diary;
import com.example.egobook_be.domain.ego_room.dto.WordCloudDto;
import com.example.egobook_be.domain.ego_room.dto.WordCloudResDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WordCloudService {

    private final WordCloudAiService wordCloudAiService;

    public List<WordCloudDto> calculateWordCloud(List<Diary> diaries) {
        if (diaries == null || diaries.isEmpty()) {
            return Collections.emptyList();
        }

        // 일기 내용 하나로 합치기
        String allContent = diaries.stream()
                .map(Diary::getContent)
                .filter(Objects::nonNull)
                .collect(Collectors.joining("\n"));

        // AI가 키워드 추출
        try {
            WordCloudResDto response = wordCloudAiService.extractKeywords(allContent);

            // 결과가 null이거나 리스트가 없으면 빈 리스트 반환
            if (response == null || response.getKeywords() == null) {
                return Collections.emptyList();
            }

            return response.getKeywords();
        } catch (Exception e) {
            log.error("AI 워드클라우드 생성 중 오류 발생: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}