package com.example.egobook_be.domain.letter;

import com.example.egobook_be.domain.letters.dto.response.WordDetectResponse;
import com.example.egobook_be.domain.letters.service.WordClientService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class WordClientServiceTest {

    @InjectMocks
    private WordClientService wordClientService;

    @Mock
    private WebClient wordRestClient;

    @Test
    @DisplayName("shouldBlock_응답이 없으면 차단한다")
    void shouldBlock_nullResponse_true() {
        assertThat(wordClientService.shouldBlock(null)).isTrue();
    }

    @Test
    @DisplayName("shouldBlock_유해 문장이고 80퍼센트 이상이면 차단한다")
    void shouldBlock_harmfulAndAboveThreshold_true() {
        WordDetectResponse response = new WordDetectResponse();
        ReflectionTestUtils.setField(response, "harmful", true);
        ReflectionTestUtils.setField(response, "percentage", 80.0d);

        assertThat(wordClientService.shouldBlock(response)).isTrue();
    }

    @Test
    @DisplayName("shouldBlock_유해 문장이 아니거나 임계치 미만이면 차단하지 않는다")
    void shouldBlock_notHarmfulOrBelowThreshold_false() {
        WordDetectResponse response = new WordDetectResponse();
        ReflectionTestUtils.setField(response, "harmful", true);
        ReflectionTestUtils.setField(response, "percentage", 79.9d);

        assertThat(wordClientService.shouldBlock(response)).isFalse();
    }
}
