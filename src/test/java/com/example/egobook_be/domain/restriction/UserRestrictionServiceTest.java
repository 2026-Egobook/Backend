package com.example.egobook_be.domain.restriction;

import com.example.egobook_be.domain.restriction.dto.MyRestrictionResDto;
import com.example.egobook_be.domain.restriction.entity.Restriction;
import com.example.egobook_be.domain.restriction.enums.RestrictionDomainType;
import com.example.egobook_be.domain.restriction.enums.RestrictionStatus;
import com.example.egobook_be.domain.restriction.mapper.RestrictionMapper;
import com.example.egobook_be.domain.restriction.repository.RestrictionRepository;
import com.example.egobook_be.domain.restriction.service.UserRestrictionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class UserRestrictionServiceTest {

    private static final Long USER_ID = 1L;

    @InjectMocks
    private UserRestrictionService userRestrictionService;

    @Mock
    private RestrictionRepository restrictionRepository;

    @Spy
    private RestrictionMapper restrictionMapper = new RestrictionMapper();

    @Test
    @DisplayName("내 제재 상태 조회 성공 - 신고 누적 자동 제재는 두 기능 모두 제재 중이고 사유가 표시 문구로 바뀐다")
    void 내_제재_상태_조회_성공_자동_제재() {
        // given
        Restriction letterRestriction = Restriction.createAutomatic(USER_ID, RestrictionDomainType.LETTER, "자동 제재");
        Restriction answerRestriction = Restriction.createAutomatic(USER_ID, RestrictionDomainType.QUESTION_ANSWER, "자동 제재");
        givenActiveRestriction(RestrictionDomainType.LETTER, letterRestriction);
        givenActiveRestriction(RestrictionDomainType.QUESTION_ANSWER, answerRestriction);

        // when
        MyRestrictionResDto result = userRestrictionService.getMyRestrictions(USER_ID);

        // then
        assertThat(result.letter().restricted()).isTrue();
        assertThat(result.letter().reason()).isEqualTo("커뮤니티 이용 규칙 위반 (반복된 신고 접수)");
        assertThat(result.letter().restrictedUntil()).isEqualTo(letterRestriction.getRestrictionUntil());
        assertThat(result.questionAnswer().restricted()).isTrue();
        assertThat(result.questionAnswer().reason()).isEqualTo("커뮤니티 이용 규칙 위반 (반복된 신고 접수)");
        assertThat(result.questionAnswer().restrictedUntil()).isEqualTo(answerRestriction.getRestrictionUntil());
    }

    @Test
    @DisplayName("내 제재 상태 조회 성공 - 관리자 제재는 입력한 사유가 그대로 내려가고 해당 기능만 제재 중이다")
    void 내_제재_상태_조회_성공_관리자_제재_편지만() {
        // given
        Restriction letterRestriction = Restriction.create(
                99L, USER_ID, RestrictionDomainType.LETTER, "반복적인 욕설 사용", "설명");
        givenActiveRestriction(RestrictionDomainType.LETTER, letterRestriction);
        givenNoRestriction(RestrictionDomainType.QUESTION_ANSWER);

        // when
        MyRestrictionResDto result = userRestrictionService.getMyRestrictions(USER_ID);

        // then
        assertThat(result.letter().restricted()).isTrue();
        assertThat(result.letter().reason()).isEqualTo("반복적인 욕설 사용");
        assertThat(result.questionAnswer().restricted()).isFalse();
        assertThat(result.questionAnswer().reason()).isNull();
        assertThat(result.questionAnswer().restrictedUntil()).isNull();
    }

    @Test
    @DisplayName("내 제재 상태 조회 성공 - 제재가 없으면 두 기능 모두 restricted=false 이다")
    void 내_제재_상태_조회_성공_제재_없음() {
        // given
        givenNoRestriction(RestrictionDomainType.LETTER);
        givenNoRestriction(RestrictionDomainType.QUESTION_ANSWER);

        // when
        MyRestrictionResDto result = userRestrictionService.getMyRestrictions(USER_ID);

        // then
        assertThat(result.letter().restricted()).isFalse();
        assertThat(result.letter().reason()).isNull();
        assertThat(result.letter().restrictedUntil()).isNull();
        assertThat(result.questionAnswer().restricted()).isFalse();
    }

    private void givenActiveRestriction(RestrictionDomainType domainType, Restriction restriction) {
        given(restrictionRepository
                .findFirstByUserIdAndDomainTypeAndStatusAndRestrictionUntilAfterOrderByRestrictionUntilDesc(
                        eq(USER_ID), eq(domainType), eq(RestrictionStatus.ACTIVE), any(LocalDateTime.class)))
                .willReturn(Optional.of(restriction));
    }

    private void givenNoRestriction(RestrictionDomainType domainType) {
        given(restrictionRepository
                .findFirstByUserIdAndDomainTypeAndStatusAndRestrictionUntilAfterOrderByRestrictionUntilDesc(
                        eq(USER_ID), eq(domainType), eq(RestrictionStatus.ACTIVE), any(LocalDateTime.class)))
                .willReturn(Optional.empty());
    }
}
