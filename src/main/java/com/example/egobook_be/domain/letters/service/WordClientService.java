package com.example.egobook_be.domain.letters.service;

import com.example.egobook_be.domain.letters.dto.request.WordDetectRequest;
import com.example.egobook_be.domain.letters.dto.response.WordDetectResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class WordClientService {

    private static final double BLOCK_THRESHOLD = 80.0;

    private final RestClient wordRestClient;

    public WordDetectResponse detect(String text) {
        return wordRestClient.post()
                .uri("/detect")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(new WordDetectRequest(text))
                .retrieve()
                .body(WordDetectResponse.class);
    }

    public boolean shouldBlock(WordDetectResponse res) {
        if (res == null) return true; // 응답 없으면 차단
        return res.isHarmful() && res.getPercentage() >= BLOCK_THRESHOLD;
    }
}
