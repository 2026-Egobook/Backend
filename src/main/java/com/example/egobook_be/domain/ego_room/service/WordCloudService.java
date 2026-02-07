package com.example.egobook_be.domain.ego_room.service;

import com.example.egobook_be.domain.diary.entity.Diary;
import com.example.egobook_be.domain.ego_room.dto.WordCloudDto;
import kr.co.shineware.nlp.komoran.constant.DEFAULT_MODEL;
import kr.co.shineware.nlp.komoran.core.Komoran;
import kr.co.shineware.nlp.komoran.model.KomoranResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
public class WordCloudService {
    private final Komoran komoran;

    public WordCloudService() {
        this.komoran = new Komoran(DEFAULT_MODEL.FULL);
    }


    public List<WordCloudDto> calculateWordCloud(List<Diary> diaries) {
        String allContent = diaries.stream()
                .map(Diary::getContent)
                .filter(Objects::nonNull)
                .collect(Collectors.joining(" "));

        // 코모란 형태소 분석
        KomoranResult analyzeResult = komoran.analyze(allContent);
        List<String> nouns = analyzeResult.getNouns();

        // 빈도수 계산 (2글자 이상 명사만)
        Map<String, Long> wordCounts = nouns.stream()
                .filter(word -> word.length() >= 2)
                .collect(Collectors.groupingBy(word -> word, Collectors.counting()));

        // 상위 30개 추출
        List<WordCloudDto> result = wordCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(30)
                .map(entry -> new WordCloudDto(entry.getKey(), entry.getValue().intValue()))
                .toList();

        return result;
    }
}