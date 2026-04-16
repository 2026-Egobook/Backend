package com.example.egobook_be.domain.letter;

import com.example.egobook_be.domain.letters.entity.PlazaLetter;
import com.example.egobook_be.domain.letters.entity.PlazaLetterMode;
import com.example.egobook_be.domain.letters.entity.PlazaLetterStatus;
import com.example.egobook_be.domain.letters.enums.PlazaLetterColor;
import com.example.egobook_be.domain.letters.repository.PlazaLetterRepository;
import com.example.egobook_be.domain.letters.service.PlazaLetterDispatchService;
import com.example.egobook_be.domain.restriction.enums.RestrictionDomainType;
import com.example.egobook_be.domain.restriction.service.RestrictionGuardService;
import com.example.egobook_be.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

// PlazaLetterDispatchService - 수신자 풀 제재 필터링 단위 테스트
@ExtendWith(MockitoExtension.class)
class PlazaLetterDispatchServiceTest {

    @Mock private PlazaLetterRepository plazaLetterRepository;
    @Mock private UserRepository userRepository;
    @Mock private RestrictionGuardService restrictionGuardService;

    @InjectMocks
    private PlazaLetterDispatchService plazaLetterDispatchService;

    private PlazaLetter buildWaitingLetter(Long letterId, Long senderId) {
        return PlazaLetter.builder()
                .letterId(letterId)
                .senderId(senderId)
                .mode(PlazaLetterMode.RANDOM)
                .fromLabel("익명")
                .content("편지 내용")
                .backgroundColor(PlazaLetterColor.WHITE)
                .status(PlazaLetterStatus.WAITING)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("대기 중인 편지가 없으면 수신자 탐색을 수행하지 않고 종료한다")
    void dispatchWaitingLetters_noWaitingLetters_earlyReturn() {
        // given
        given(plazaLetterRepository.findWaitingLetters(any(PageRequest.class)))
                .willReturn(Collections.emptyList());

        // when
        plazaLetterDispatchService.dispatchWaitingLetters();

        // then
        verify(userRepository, never()).findAvailableReceivers(any(), any());
    }

    @Test
    @DisplayName("수신 가능한 사용자가 모두 제재 상태일 경우 편지를 배정하지 않는다")
    void dispatchWaitingLetters_allReceiversRestricted_noAssignment() {
        // given
        Long senderId = 1L;
        Long restrictedReceiverId = 2L;
        PlazaLetter letter = buildWaitingLetter(100L, senderId);

        given(plazaLetterRepository.findWaitingLetters(any(PageRequest.class)))
                .willReturn(List.of(letter));
        given(userRepository.findAvailableReceivers(any(LocalDateTime.class), any(PageRequest.class)))
                .willReturn(List.of(restrictedReceiverId));
        given(restrictionGuardService.getActivelyRestrictedUserIds(RestrictionDomainType.LETTER))
                .willReturn(new HashSet<>(Set.of(restrictedReceiverId)));

        // when
        plazaLetterDispatchService.dispatchWaitingLetters();

        // then — receiver pool이 비어 있으므로 수신자 미배정
        assertThat(letter.getReceiverId()).isNull();
    }

    @Test
    @DisplayName("제재 상태인 사용자를 제외하고, 정상적인 사용자에게만 편지를 배정한다")
    void dispatchWaitingLetters_partialRestriction_assignsNonRestrictedReceiver() {
        // given
        Long senderId = 1L;
        Long restrictedId = 2L;
        Long validReceiverId = 3L;
        PlazaLetter letter = buildWaitingLetter(100L, senderId);

        given(plazaLetterRepository.findWaitingLetters(any(PageRequest.class)))
                .willReturn(List.of(letter));
        given(userRepository.findAvailableReceivers(any(LocalDateTime.class), any(PageRequest.class)))
                .willReturn(List.of(restrictedId, validReceiverId));
        given(restrictionGuardService.getActivelyRestrictedUserIds(RestrictionDomainType.LETTER))
                .willReturn(new HashSet<>(Set.of(restrictedId)));

        // when
        plazaLetterDispatchService.dispatchWaitingLetters();

        // then — 제재된 id(2L)가 아닌 유효한 수신자(3L)에게만 배정 가능
        assertThat(letter.getReceiverId()).isEqualTo(validReceiverId);
    }
}