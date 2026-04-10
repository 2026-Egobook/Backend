package com.example.egobook_be.domain.restriction.service;

import com.example.egobook_be.domain.restriction.dto.RestrictionItemResDto;
import com.example.egobook_be.domain.restriction.entity.Restriction;
import com.example.egobook_be.domain.restriction.enums.RestrictionDomainType;
import com.example.egobook_be.domain.restriction.enums.RestrictionStatus;
import com.example.egobook_be.domain.restriction.repository.RestrictionRepository;
import com.example.egobook_be.domain.user.entity.User;
import com.example.egobook_be.domain.user.enums.UserStatus;
import com.example.egobook_be.domain.user.repository.UserRepository;
import com.example.egobook_be.global.enums.ReportReason;
import com.example.egobook_be.global.exception.CustomException;
import com.example.egobook_be.global.response.SliceResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// [AI-GEN] AdminRestrictionService Integration Test
@SpringBootTest
@Transactional
@ActiveProfiles("test")
public class AdminRestrictionServiceIntegrationTest {

    @Autowired
    private AdminRestrictionService adminRestrictionService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RestrictionRepository restrictionRepository;

    private Long targetUserId;
    private Long otherUserId;

    @BeforeEach
    void setUp() {
        User targetUser = userRepository.save(
                User.builder()
                        .accountCode("EG100")
                        .email("target@test.com")
                        .nickname("대상사용자")
                        .status(UserStatus.ACTIVE)
                        .build()
        );
        User otherUser = userRepository.save(
                User.builder()
                        .accountCode("EG101")
                        .email("other@test.com")
                        .nickname("다른사용자")
                        .status(UserStatus.ACTIVE)
                        .build()
        );
        targetUserId = targetUser.getId();
        otherUserId = otherUser.getId();

        // 대상 사용자: ACTIVE 2건, CANCELED 1건
        restrictionRepository.save(buildRestriction(targetUserId, RestrictionDomainType.LETTER, RestrictionStatus.ACTIVE));
        restrictionRepository.save(buildRestriction(targetUserId, RestrictionDomainType.QUESTION_ANSWER, RestrictionStatus.ACTIVE));
        restrictionRepository.save(buildRestriction(targetUserId, RestrictionDomainType.LETTER, RestrictionStatus.CANCELED));

        // 다른 사용자: ACTIVE 1건 (결과에 포함되면 안 됨)
        restrictionRepository.save(buildRestriction(otherUserId, RestrictionDomainType.LETTER, RestrictionStatus.ACTIVE));
    }

    @Test
    @DisplayName("[통합] status=null이면 대상 사용자의 전체 3건을 조회한다")
    void getRestrictionList_AllStatus() {
        // ============ When =================
        SliceResponse<RestrictionItemResDto> result =
                adminRestrictionService.getRestrictionList(targetUserId, 1, 10, null);

        // ============ Then =================
        assertThat(result.content()).hasSize(3);
        assertThat(result.hasNext()).isFalse();
    }

    @Test
    @DisplayName("[통합] status=ACTIVE이면 ACTIVE 건 2건만 조회한다")
    void getRestrictionList_FilterByActive() {
        // ============ When =================
        SliceResponse<RestrictionItemResDto> result =
                adminRestrictionService.getRestrictionList(targetUserId, 1, 10, RestrictionStatus.ACTIVE);

        // ============ Then =================
        assertThat(result.content()).hasSize(2);
        assertThat(result.content())
                .extracting("restrictionStatus")
                .containsOnly(RestrictionStatus.ACTIVE);
    }

    @Test
    @DisplayName("[통합] status=CANCELED이면 CANCELED 건 1건만 조회한다")
    void getRestrictionList_FilterByCanceled() {
        // ============ When =================
        SliceResponse<RestrictionItemResDto> result =
                adminRestrictionService.getRestrictionList(targetUserId, 1, 10, RestrictionStatus.CANCELED);

        // ============ Then =================
        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).restrictionStatus()).isEqualTo(RestrictionStatus.CANCELED);
    }

    @Test
    @DisplayName("[통합] 다른 userId의 제재 기록은 결과에 포함되지 않는다")
    void getRestrictionList_ExcludesOtherUser() {
        // ============ When =================
        SliceResponse<RestrictionItemResDto> result =
                adminRestrictionService.getRestrictionList(targetUserId, 1, 10, null);

        // ============ Then =================
        assertThat(result.content())
                .extracting("restrictionId")
                .doesNotContain(otherUserId);
        assertThat(result.content()).hasSize(3); // 다른 userId 건 제외됨
    }

    @Test
    @DisplayName("[통합] page=1, size=2이면 hasNext=true 반환")
    void getRestrictionList_HasNextIsTrue() {
        // ============ When =================
        SliceResponse<RestrictionItemResDto> result =
                adminRestrictionService.getRestrictionList(targetUserId, 1, 2, null);

        // ============ Then =================
        assertThat(result.content()).hasSize(2);
        assertThat(result.hasNext()).isTrue();
        assertThat(result.page()).isEqualTo(1);
        assertThat(result.size()).isEqualTo(2);
    }

    @Test
    @DisplayName("[통합] page=2, size=2이면 나머지 1건 조회 후 hasNext=false 반환")
    void getRestrictionList_HasNextIsFalse() {
        // ============ When =================
        SliceResponse<RestrictionItemResDto> result =
                adminRestrictionService.getRestrictionList(targetUserId, 2, 2, null);

        // ============ Then =================
        assertThat(result.content()).hasSize(1);
        assertThat(result.hasNext()).isFalse();
        assertThat(result.page()).isEqualTo(2);
    }

    @Test
    @DisplayName("[통합] 존재하지 않는 userId이면 CustomException 발생")
    void getRestrictionList_UserNotFound() {
        // ============ When & Then =================
        assertThatThrownBy(() -> adminRestrictionService.getRestrictionList(999L, 1, 10, null))
                .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("[통합] 응답 DTO 필드가 Restriction 엔티티와 정확히 매핑된다")
    void getRestrictionList_VerifyDtoFieldMapping() {
        // ============ When =================
        SliceResponse<RestrictionItemResDto> result =
                adminRestrictionService.getRestrictionList(targetUserId, 1, 10, RestrictionStatus.CANCELED);

        // ============ Then =================
        RestrictionItemResDto dto = result.content().get(0);
        assertThat(dto.restrictionId()).isNotNull();
        assertThat(dto.domainType()).isEqualTo(RestrictionDomainType.LETTER);
        assertThat(dto.reason()).isEqualTo(ReportReason.ABUSE);
        assertThat(dto.description()).isEqualTo("테스트 제재 사유");
        assertThat(dto.restrictionStatus()).isEqualTo(RestrictionStatus.CANCELED);
        assertThat(dto.createdAt()).isNotNull();
        assertThat(dto.restrictionUntil()).isNotNull();
    }

    // =========================================================================
    // Helper methods
    // =========================================================================

    private Restriction buildRestriction(Long userId, RestrictionDomainType domainType, RestrictionStatus status) {
        return Restriction.builder()
                .adminId(1L)
                .userId(userId)
                .domainType(domainType)
                .reason(ReportReason.ABUSE)
                .description("테스트 제재 사유")
                .status(status)
                .restrictionUntil(LocalDateTime.now().plusDays(7))
                .build();
    }
}
