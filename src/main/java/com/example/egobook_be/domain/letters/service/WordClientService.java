package com.example.egobook_be.domain.letters.service;

import com.example.egobook_be.domain.letters.dto.request.WordDetectRequest;
import com.example.egobook_be.domain.letters.dto.response.WordDetectResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class WordClientService {

    private static final double BLOCK_THRESHOLD = 80.0;

    private final WebClient wordRestClient;


    public WordDetectResponse detect(String text) {
        return wordRestClient.post()
                .uri("/detect")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(new WordDetectRequest(text))
                .retrieve()
                .bodyToMono(WordDetectResponse.class)
                .block();
    }


    public Mono<WordDetectResponse> detectAsync(String text) {
        return wordRestClient.post()
                .uri("/detect")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(new WordDetectRequest(text))
                .retrieve()
                .bodyToMono(WordDetectResponse.class);
    }

    public boolean shouldBlock(WordDetectResponse res) {
        if (res == null) return true;
        return res.isHarmful() && res.getPercentage() >= BLOCK_THRESHOLD;
    }
}