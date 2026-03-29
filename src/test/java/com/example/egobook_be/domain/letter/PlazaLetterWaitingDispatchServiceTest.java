package com.example.egobook_be.domain.letter;

import com.example.egobook_be.domain.letters.entity.PlazaLetter;
import com.example.egobook_be.domain.letters.entity.PlazaLetterMode;
import com.example.egobook_be.domain.letters.entity.PlazaLetterStatus;
import com.example.egobook_be.domain.letters.repository.PlazaLetterRepository;
import com.example.egobook_be.domain.letters.service.scheduler.PlazaLetterWaitingDispatchService;
import com.example.egobook_be.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class PlazaLetterWaitingDispatchServiceTest {

    @InjectMocks
    private PlazaLetterWaitingDispatchService plazaLetterWaitingDispatchService;

    @Mock
    private PlazaLetterRepository plazaLetterRepository;

    @Mock
    private UserRepository userRepository;

    @Test
    @DisplayName("dispatchWaitingLetters_수신 가능 유저가 없으면 0을 반환한다")
    void dispatchWaitingLetters_noCandidateUsers_returnZero() {
        given(userRepository.findReceivableUsers(any(), any())).willReturn(List.of());

        int result = plazaLetterWaitingDispatchService.dispatchWaitingLetters();

        assertThat(result).isZero();
    }

    @Test
    @DisplayName("dispatchWaitingLetters_WAITING 편지를 수신 가능 유저에게 배정한다")
    void dispatchWaitingLetters_waitingLettersExist_assignArrived() {
        PlazaLetter waiting1 = waitingLetter(1L);
        PlazaLetter waiting2 = waitingLetter(2L);

        given(userRepository.findReceivableUsers(any(), any())).willReturn(List.of(101L, 102L));
        given(plazaLetterRepository.findWaitingLetters(any())).willReturn(List.of(waiting1, waiting2));

        int result = plazaLetterWaitingDispatchService.dispatchWaitingLetters();

        assertThat(result).isEqualTo(2);
        assertThat(waiting1.getReceiverId()).isEqualTo(101L);
        assertThat(waiting1.getStatus()).isEqualTo(PlazaLetterStatus.ARRIVED);
        assertThat(waiting1.getArrivedAt()).isNotNull();
        assertThat(waiting1.getReplyDeadlineAt()).isNotNull();

        assertThat(waiting2.getReceiverId()).isEqualTo(102L);
        assertThat(waiting2.getStatus()).isEqualTo(PlazaLetterStatus.ARRIVED);
    }

    private PlazaLetter waitingLetter(Long letterId) {
        return PlazaLetter.builder()
                .letterId(letterId)
                .threadId(400L + letterId)
                .senderId(10L + letterId)
                .receiverId(null)
                .mode(PlazaLetterMode.RANDOM)
                .fromLabel("익명")
                .content("대기 편지")
                .status(PlazaLetterStatus.WAITING)
                .createdAt(LocalDateTime.now().minusHours(2))
                .build();
    }
}
