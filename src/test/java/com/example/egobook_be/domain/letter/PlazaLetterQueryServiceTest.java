package com.example.egobook_be.domain.letter;

import com.example.egobook_be.domain.letters.enums.LettersErrorCode;
import com.example.egobook_be.domain.letters.repository.PlazaLetterRepository;
import com.example.egobook_be.domain.letters.service.PlazaLetterQueryService;
import com.example.egobook_be.domain.restriction.exception.RestrictionErrorCode;
import com.example.egobook_be.domain.restriction.service.RestrictionGuardService;
import com.example.egobook_be.global.exception.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.*;

// PlazaLetterQueryService - getInboxLetterDetail 제재 관련 단위 테스트
@ExtendWith(MockitoExtension.class)
class PlazaLetterQueryServiceTest {

    @Mock private PlazaLetterRepository plazaLetterRepository;
    @Mock private RestrictionGuardService restrictionGuardService;

    @InjectMocks
    private PlazaLetterQueryService plazaLetterQueryService;

    @Test
    @DisplayName("편지 이용이 제한된 사용자가 수신함 상세 조회를 요청하면 예외가 발생한다")
    void getInboxLetterDetail_letterRestricted_throwsException() {
        // given
        Long userId = 1L;
        Long letterId = 100L;
        willThrow(new CustomException(RestrictionErrorCode.LETTER_RESTRICTED))
                .given(restrictionGuardService).checkLetterRestriction(userId);

        // when & then
        assertThatThrownBy(() -> plazaLetterQueryService.getInboxLetterDetail(userId, letterId))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(RestrictionErrorCode.LETTER_RESTRICTED);

        verify(plazaLetterRepository, never()).findInboxLetterForReply(anyLong(), anyLong());
    }

    @Test
    @DisplayName("조회하려는 편지가 존재하지 않으면 예외가 발생한다")
    void getInboxLetterDetail_letterNotFound_throwsException() {
        // given
        Long userId = 1L;
        Long letterId = 999L;
        given(plazaLetterRepository.findInboxLetterForReply(letterId, userId))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> plazaLetterQueryService.getInboxLetterDetail(userId, letterId))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(LettersErrorCode.LETTER_NOT_FOUND);
    }
}