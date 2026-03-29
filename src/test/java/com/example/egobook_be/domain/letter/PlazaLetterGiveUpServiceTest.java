package com.example.egobook_be.domain.letter;

import com.example.egobook_be.domain.letters.entity.PlazaLetter;
import com.example.egobook_be.domain.letters.entity.PlazaLetterMode;
import com.example.egobook_be.domain.letters.entity.PlazaLetterStatus;
import com.example.egobook_be.domain.letters.repository.PlazaLetterRepository;
import com.example.egobook_be.domain.letters.service.scheduler.PlazaLetterGiveUpService;
import com.example.egobook_be.domain.user.entity.User;
import com.example.egobook_be.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PlazaLetterGiveUpServiceTest {

    @InjectMocks
    private PlazaLetterGiveUpService plazaLetterGiveUpService;

    @Mock
    private PlazaLetterRepository plazaLetterRepository;

    @Mock
    private UserRepository userRepository;

    @Test
    @DisplayName("autoGiveUpExpiredLetters_대상이 없으면 0을 반환한다")
    void autoGiveUpExpiredLetters_noTargets_returnZero() {
        given(plazaLetterRepository.findGiveUpTargets(any(), any(), any(), any())).willReturn(List.of());

        int result = plazaLetterGiveUpService.autoGiveUpExpiredLetters();

        assertThat(result).isZero();
        verify(userRepository, never()).findById(any());
    }

    @Test
    @DisplayName("autoGiveUpExpiredLetters_만료 편지를 포기 처리하고 4시간 수신 차단한다")
    void autoGiveUpExpiredLetters_expiredLetters_markGiveUpAndBlockReceiver() {
        PlazaLetter target = plazaLetter(10L, 2L, 1L, PlazaLetterStatus.ARRIVED);
        User receiver = user(1L);

        given(plazaLetterRepository.findGiveUpTargets(any(), any(), any(), any())).willReturn(List.of(target));
        given(userRepository.findById(1L)).willReturn(Optional.of(receiver));

        int result = plazaLetterGiveUpService.autoGiveUpExpiredLetters();

        assertThat(result).isEqualTo(1);
        assertThat(target.getStatus()).isEqualTo(PlazaLetterStatus.GAVE_UP);
        assertThat(target.getGaveUpAt()).isNotNull();
        assertThat(receiver.getLetterReceiveBlockedUntil()).isNotNull();
    }

    private PlazaLetter plazaLetter(Long letterId, Long senderId, Long receiverId, PlazaLetterStatus status) {
        return PlazaLetter.builder()
                .letterId(letterId)
                .threadId(300L)
                .senderId(senderId)
                .receiverId(receiverId)
                .mode(PlazaLetterMode.RANDOM)
                .fromLabel("익명")
                .content("편지")
                .status(status)
                .createdAt(LocalDateTime.now().minusDays(1))
                .arrivedAt(LocalDateTime.now().minusHours(25))
                .replyDeadlineAt(LocalDateTime.now().minusHours(1))
                .build();
    }

    private User user(Long userId) {
        return User.builder()
                .id(userId)
                .accountCode("USER1234")
                .nickname("receiver")
                .ink(0)
                .build();
    }
}
