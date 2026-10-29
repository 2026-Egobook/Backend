package com.example.egobook_be.domain.home.service;

import com.example.egobook_be.domain.home.dto.TurtleMessageResDto;
import com.example.egobook_be.domain.home.entity.TurtleMessage;
import com.example.egobook_be.domain.home.mapper.HomeMapper;
import com.example.egobook_be.domain.home.repository.TurtleMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TurtleMessageService {

    private final TurtleMessageRepository turtleMessageRepository;
    private final HomeMapper homeMapper;

    /**
     * 고북이 말풍선 문구 1개를 무작위로 조회
     * - 호출할 때마다 DB에 등록된 문구 중 하나를 무작위로 골라 반환한다.
     * - 등록된 문구가 없으면 message 가 null 인 응답을 반환한다.
     * @return : 말풍선 문구를 담은 TurtleMessageResDto
     */
    @Transactional(readOnly = true)
    public TurtleMessageResDto getRandomTurtleMessage() {
        log.info("[TurtleMessageService] getRandomTurtleMessage() - START");

        String message = turtleMessageRepository.findRandom()
                .map(TurtleMessage::getContent)
                .orElse(null);
        TurtleMessageResDto result = homeMapper.toTurtleMessageResDto(message);

        log.info("[TurtleMessageService] getRandomTurtleMessage() - END | hasMessage: {}", message != null);
        return result;
    }
}
