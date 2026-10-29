package com.example.egobook_be.domain.home.service;

import com.example.egobook_be.domain.home.dto.TurtleMessageResDto;
import com.example.egobook_be.domain.home.entity.TurtleMessage;
import com.example.egobook_be.domain.home.mapper.HomeMapper;
import com.example.egobook_be.domain.home.repository.TurtleMessageRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class TurtleMessageServiceTest {

    @InjectMocks
    private TurtleMessageService turtleMessageService;

    @Mock
    private TurtleMessageRepository turtleMessageRepository;

    @Spy
    private HomeMapper homeMapper = new HomeMapper();

    @Test
    @DisplayName("고북이 말풍선 문구 조회 성공 - 랜덤으로 뽑힌 문구가 반환된다")
    void 고북이_말풍선_문구_조회_성공() {
        // given
        given(turtleMessageRepository.findRandom()).willReturn(Optional.of(
                TurtleMessage.builder().id(3L).content("지금 기분은 어때요?").build()
        ));

        // when
        TurtleMessageResDto result = turtleMessageService.getRandomTurtleMessage();

        // then
        assertThat(result.message()).isEqualTo("지금 기분은 어때요?");
    }

    @Test
    @DisplayName("고북이 말풍선 문구 조회 성공 - 등록된 문구가 없으면 message 가 null 이다")
    void 고북이_말풍선_문구_조회_성공_문구_없음() {
        // given
        given(turtleMessageRepository.findRandom()).willReturn(Optional.empty());

        // when
        TurtleMessageResDto result = turtleMessageService.getRandomTurtleMessage();

        // then
        assertThat(result.message()).isNull();
    }
}
