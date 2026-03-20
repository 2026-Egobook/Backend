package com.example.egobook_be.domain.letter;

import com.example.egobook_be.domain.letters.entity.PlazaLetter;
import com.example.egobook_be.domain.letters.entity.PlazaLetterMode;
import com.example.egobook_be.domain.letters.entity.PlazaLetterReply;
import com.example.egobook_be.domain.letters.entity.PlazaLetterStatus;
import com.example.egobook_be.domain.letters.repository.PlazaLetterReplyRepository;
import com.example.egobook_be.domain.letters.repository.PlazaLetterRepository;
import com.example.egobook_be.domain.letters.service.ai.GeminiClient;
import com.example.egobook_be.domain.letters.service.ai.PlazaLetterAiReplyService;
import com.example.egobook_be.domain.notification.service.NotificationService;
import com.example.egobook_be.domain.user.entity.User;
import com.example.egobook_be.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PlazaLetterAiReplyServiceTest {

    @InjectMocks
    private PlazaLetterAiReplyService plazaLetterAiReplyService;

    @Mock
    private PlazaLetterRepository plazaLetterRepository;

    @Mock
    private PlazaLetterReplyRepository plazaLetterReplyRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private GeminiClient geminiClient;

    @Mock
    private NotificationService notificationService;

    @Test
    @DisplayName("generateAiReplyIfEligible_이미 답장이 있으면 false를 반환한다")
    void generateAiReplyIfEligible_replyAlreadyExists_returnFalse() {
        PlazaLetter letter = targetLetter(10L, OffsetDateTime.now().minusHours(49), PlazaLetterStatus.ARRIVED);
        given(plazaLetterRepository.findById(10L)).willReturn(Optional.of(letter));
        given(plazaLetterReplyRepository.existsByLetter(letter)).willReturn(true);

        boolean result = plazaLetterAiReplyService.generateAiReplyIfEligible(10L);

        assertThat(result).isFalse();
        verify(geminiClient, never()).generateReply(any(), any());
    }

    @Test
    @DisplayName("generateAiReplyIfEligible_48시간이 지나지 않았으면 false를 반환한다")
    void generateAiReplyIfEligible_notExpired48Hours_returnFalse() {
        PlazaLetter letter = targetLetter(10L, OffsetDateTime.now().minusHours(47), PlazaLetterStatus.ARRIVED);
        given(plazaLetterRepository.findById(10L)).willReturn(Optional.of(letter));
        given(plazaLetterReplyRepository.existsByLetter(letter)).willReturn(false);

        boolean result = plazaLetterAiReplyService.generateAiReplyIfEligible(10L);

        assertThat(result).isFalse();
        verify(geminiClient, never()).generateReply(any(), any());
    }

    @Test
    @DisplayName("generateAiReplyIfEligible_조건을 만족하면 AI 답장을 저장하고 상태를 AI_REPLIED로 변경한다")
    void generateAiReplyIfEligible_eligibleLetter_success() {
        PlazaLetter letter = targetLetter(10L, OffsetDateTime.now().minusHours(49), PlazaLetterStatus.ARRIVED);
        User sender = User.builder()
                .id(1L)
                .accountCode("USER1234")
                .nickname("효진")
                .ink(0)
                .build();

        given(plazaLetterRepository.findById(10L)).willReturn(Optional.of(letter));
        given(plazaLetterReplyRepository.existsByLetter(letter)).willReturn(false);
        given(userRepository.findById(1L)).willReturn(Optional.of(sender));
        given(geminiClient.generateReply(eq("효진"), eq("원본 편지 내용")))
                .willReturn("첫 줄입니다. 둘째 줄입니다. 셋째 줄입니다.");
        given(plazaLetterReplyRepository.save(any(PlazaLetterReply.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        boolean result = plazaLetterAiReplyService.generateAiReplyIfEligible(10L);

        assertThat(result).isTrue();
        assertThat(letter.getStatus()).isEqualTo(PlazaLetterStatus.AI_REPLIED);
        assertThat(letter.getRepliedAt()).isNotNull();
        verify(plazaLetterReplyRepository).save(any(PlazaLetterReply.class));
        verify(plazaLetterRepository).save(letter);
    }

    private PlazaLetter targetLetter(Long letterId, OffsetDateTime createdAt, PlazaLetterStatus status) {
        return PlazaLetter.builder()
                .letterId(letterId)
                .threadId(500L)
                .senderId(1L)
                .receiverId(2L)
                .mode(PlazaLetterMode.RANDOM)
                .fromLabel("익명")
                .content("원본 편지 내용")
                .status(status)
                .createdAt(createdAt)
                .arrivedAt(createdAt.plusHours(1))
                .replyDeadlineAt(createdAt.plusHours(25))
                .build();
    }
}
