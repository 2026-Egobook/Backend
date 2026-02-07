package com.example.egobook_be.domain.letters.service.ai;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Component
public class GeminiClient {

    private final WebClient webClient;
    private final String apiKey;
    private final String model;
    private final String baseUrl;

    public GeminiClient(WebClient.Builder webClientBuilder,
                        @Value("${gemini.base-url}") String baseUrl,
                        @Value("${gemini.api-key}") String apiKey,
                        @Value("${gemini.model}") String model) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build(); // WebClient 설정
        this.apiKey = apiKey;
        this.model = model;
        this.baseUrl = baseUrl;
    }

    public String generateReply(String nickname, String letterContent) {
        String prompt = buildPrompt(nickname, letterContent);

        String url = baseUrl + "/v1beta/models/" + model + ":generateContent";

        Map<String, Object> body = Map.of(
                "contents", List.of(
                        Map.of("role", "user", "parts", List.of(
                                Map.of("text", prompt)
                        ))
                ),
                "generationConfig", Map.of(
                        "temperature", 0.6,
                        "maxOutputTokens", 256
                )
        );

        // WebClient로 요청 보내기
        Mono<GeminiResponse> response = webClient.post()
                .uri(url)
                .header("X-goog-api-key", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(GeminiResponse.class);

        // 응답이 비어 있지 않다면 텍스트를 반환
        GeminiResponse geminiResponse = response.block(); // block()을 통해 비동기 요청을 동기적으로 처리
        if (geminiResponse == null || geminiResponse.getText() == null || geminiResponse.getText().isBlank()) {
            throw new IllegalStateException("Gemini response is empty");
        }

        return geminiResponse.getText().trim();
    }

    private String buildPrompt(String nickname, String letterContent) {
        String safeNickname = (nickname == null) ? "" : nickname.trim();
        String safeLetter = (letterContent == null) ? "" : letterContent.trim();

        return """
                너는 "에고북" 앱에서 48시간 동안 답장이 없을 때 대신 답장을 작성하는 AI야.
                아래 규칙을 엄격하게 지키고, 답장 본문만 출력해.

                [규칙]
                1) 3~5줄로 작성
                2) 전체 360자 이내
                3) 해요체 사용
                4) 공감/이해/인정 중심으로 작성
                5) 이모지 금지, 특수문자 감정표현 금지
                6) 2인칭 대명사 사용 금지: 당신, 너, 그대, 님, 여러분 등
                7) 사용자는 닉네임으로만 지칭 가능 (예: "%s")
                8) 머리말/서명/제목/번호/따옴표/안내문 출력 금지
                9) 조언은 강요하지 말고, 부드럽고 현실적인 한두 문장만

                [닉네임]
                %s

                [원문 편지]
                %s
                """.formatted(
                safeNickname.isEmpty() ? "사용자" : safeNickname,
                safeNickname,
                safeLetter
        );
    }
}
