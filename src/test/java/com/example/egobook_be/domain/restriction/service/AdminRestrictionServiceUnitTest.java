package com.example.egobook_be.domain.restriction.service;

import com.example.egobook_be.domain.restriction.dto.RestrictionItemResDto;
import com.example.egobook_be.domain.restriction.entity.Restriction;
import com.example.egobook_be.domain.restriction.enums.RestrictionDomainType;
import com.example.egobook_be.domain.restriction.enums.RestrictionStatus;
import com.example.egobook_be.domain.restriction.mapper.RestrictionMapper;
import com.example.egobook_be.domain.restriction.repository.RestrictionRepository;
import com.example.egobook_be.domain.user.repository.UserRepository;
import com.example.egobook_be.global.enums.ReportReason;
import com.example.egobook_be.global.exception.CustomException;
import com.example.egobook_be.global.response.SliceResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

// AdminRestrictionService Unit Test
@ExtendWith(MockitoExtension.class)
public class AdminRestrictionServiceUnitTest {

    @InjectMocks
    private AdminRestrictionService adminRestrictionService;

    @Mock private UserRepository userRepository;
    @Mock private RestrictionRepository restrictionRepository;
    @Mock private RestrictionMapper restrictionMapper;

    // =========================================================================
    // getRestrictionList
    // =========================================================================
    @Nested
    class GetRestrictionListTest {

        @Test
        @DisplayName("[실패] page가 1 미만이면 INVALID_SLICE_VALUE 예외 발생")
        void failWhenPageIsLessThanOne() {
            // ============ When & Then =================
            assertThatThrownBy(() -> adminRestrictionService.getRestrictionList(1L, 0, 10, null))
                    .isInstanceOf(CustomException.class);

            assertThatThrownBy(() -> adminRestrictionService.getRestrictionList(1L, -1, 10, null))
                    .isInstanceOf(CustomException.class);
        }

        @Test
        @DisplayName("[실패] size가 1 미만이면 INVALID_SIZE_VALUE 예외 발생")
        void failWhenSizeIsLessThanOne() {
            // ============ When & Then =================
            assertThatThrownBy(() -> adminRestrictionService.getRestrictionList(1L, 1, 0, null))
                    .isInstanceOf(CustomException.class);
        }

        @Test
        @DisplayName("[실패] size가 100 초과이면 INVALID_SIZE_VALUE 예외 발생")
        void failWhenSizeExceedsMax() {
            // ============ When & Then =================
            assertThatThrownBy(() -> adminRestrictionService.getRestrictionList(1L, 1, 101, null))
                    .isInstanceOf(CustomException.class);
        }

        @Test
        @DisplayName("[실패] 존재하지 않는 userId이면 USER_NOT_FOUND 예외 발생")
        void failWhenUserNotFound() {
            // ============ Given =================
            given(userRepository.findById(999L)).willReturn(Optional.empty());

            // ============ When & Then =================
            assertThatThrownBy(() -> adminRestrictionService.getRestrictionList(999L, 1, 10, null))
                    .isInstanceOf(CustomException.class);
        }

        @Test
        @DisplayName("[성공] status=null이면 findAllByUserId 호출, findAllByUserIdAndStatus 미호출")
        void successWhenStatusIsNull() {
            // ============ Given =================
            Long userId = 1L;
            Restriction mockRestriction = mockRestriction(userId, RestrictionStatus.ACTIVE);
            Slice<Restriction> mockSlice = new SliceImpl<>(List.of(mockRestriction));
            RestrictionItemResDto mockDto = mockItemResDto();

            given(userRepository.findById(userId)).willReturn(Optional.of(mock(com.example.egobook_be.domain.user.entity.User.class)));
            given(restrictionRepository.findAllByUserId(eq(userId), any(Pageable.class))).willReturn(mockSlice);
            given(restrictionMapper.toItemResDto(any())).willReturn(mockDto);

            // ============ When =================
            SliceResponse<RestrictionItemResDto> result =
                    adminRestrictionService.getRestrictionList(userId, 1, 10, null);

            // ============ Then =================
            assertThat(result).isNotNull();
            verify(restrictionRepository, times(1)).findAllByUserId(eq(userId), any(Pageable.class));
            verify(restrictionRepository, never()).findAllByUserIdAndStatus(any(), any(), any());
        }

        @Test
        @DisplayName("[성공] status=ACTIVE이면 findAllByUserIdAndStatus(userId, ACTIVE) 호출")
        void successWhenStatusIsActive() {
            // ============ Given =================
            Long userId = 1L;
            Restriction mockRestriction = mockRestriction(userId, RestrictionStatus.ACTIVE);
            Slice<Restriction> mockSlice = new SliceImpl<>(List.of(mockRestriction));
            RestrictionItemResDto mockDto = mockItemResDto();

            given(userRepository.findById(userId)).willReturn(Optional.of(mock(com.example.egobook_be.domain.user.entity.User.class)));
            given(restrictionRepository.findAllByUserIdAndStatus(eq(userId), eq(RestrictionStatus.ACTIVE), any(Pageable.class)))
                    .willReturn(mockSlice);
            given(restrictionMapper.toItemResDto(any())).willReturn(mockDto);

            // ============ When =================
            SliceResponse<RestrictionItemResDto> result =
                    adminRestrictionService.getRestrictionList(userId, 1, 10, RestrictionStatus.ACTIVE);

            // ============ Then =================
            assertThat(result).isNotNull();
            verify(restrictionRepository, times(1))
                    .findAllByUserIdAndStatus(eq(userId), eq(RestrictionStatus.ACTIVE), any(Pageable.class));
            verify(restrictionRepository, never()).findAllByUserId(any(), any());
        }

        @Test
        @DisplayName("[성공] status=CANCELED이면 findAllByUserIdAndStatus(userId, CANCELED) 호출")
        void successWhenStatusIsCanceled() {
            // ============ Given =================
            Long userId = 1L;
            Restriction mockRestriction = mockRestriction(userId, RestrictionStatus.CANCELED);
            Slice<Restriction> mockSlice = new SliceImpl<>(List.of(mockRestriction));
            RestrictionItemResDto mockDto = mockItemResDto();

            given(userRepository.findById(userId)).willReturn(Optional.of(mock(com.example.egobook_be.domain.user.entity.User.class)));
            given(restrictionRepository.findAllByUserIdAndStatus(eq(userId), eq(RestrictionStatus.CANCELED), any(Pageable.class)))
                    .willReturn(mockSlice);
            given(restrictionMapper.toItemResDto(any())).willReturn(mockDto);

            // ============ When =================
            SliceResponse<RestrictionItemResDto> result =
                    adminRestrictionService.getRestrictionList(userId, 1, 10, RestrictionStatus.CANCELED);

            // ============ Then =================
            assertThat(result).isNotNull();
            verify(restrictionRepository, times(1))
                    .findAllByUserIdAndStatus(eq(userId), eq(RestrictionStatus.CANCELED), any(Pageable.class));
        }

        @Test
        @DisplayName("[성공] page=3, size=5일 때 Pageable의 pageNumber=2, pageSize=5, Sort=createdAt DESC")
        void successVerifyPageableArguments() {
            // ============ Given =================
            Long userId = 1L;
            Slice<Restriction> mockSlice = new SliceImpl<>(List.of());

            given(userRepository.findById(userId)).willReturn(Optional.of(mock(com.example.egobook_be.domain.user.entity.User.class)));
            given(restrictionRepository.findAllByUserId(eq(userId), any(Pageable.class))).willReturn(mockSlice);

            // ============ When =================
            adminRestrictionService.getRestrictionList(userId, 3, 5, null);

            // ============ Then =================
            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
            verify(restrictionRepository).findAllByUserId(eq(userId), pageableCaptor.capture());

            Pageable captured = pageableCaptor.getValue();
            assertThat(captured.getPageNumber()).isEqualTo(2);  // 3 - 1
            assertThat(captured.getPageSize()).isEqualTo(5);
            assertThat(captured.getSort().getOrderFor("createdAt").getDirection())
                    .isEqualTo(Sort.Direction.DESC);
        }

        @Test
        @DisplayName("[성공] SliceResponse의 content, page, size, hasNext가 올바르게 반환됨")
        void successVerifySliceResponse() {
            // ============ Given =================
            Long userId = 1L;
            RestrictionItemResDto mockDto = mockItemResDto();
            Restriction mockRestriction = mockRestriction(userId, RestrictionStatus.ACTIVE);
            Slice<Restriction> mockSlice = new SliceImpl<>(List.of(mockRestriction), org.springframework.data.domain.PageRequest.of(0, 10), false);

            given(userRepository.findById(userId)).willReturn(Optional.of(mock(com.example.egobook_be.domain.user.entity.User.class)));
            given(restrictionRepository.findAllByUserId(eq(userId), any(Pageable.class))).willReturn(mockSlice);
            given(restrictionMapper.toItemResDto(any())).willReturn(mockDto);

            // ============ When =================
            SliceResponse<RestrictionItemResDto> result =
                    adminRestrictionService.getRestrictionList(userId, 1, 10, null);

            // ============ Then =================
            assertThat(result.content()).hasSize(1);
            assertThat(result.content().get(0)).isEqualTo(mockDto);
            assertThat(result.page()).isEqualTo(1);
            assertThat(result.size()).isEqualTo(10);
            assertThat(result.hasNext()).isFalse();
        }
    }

    // =========================================================================
    // Helper methods
    // =========================================================================

    private Restriction mockRestriction(Long userId, RestrictionStatus status) {
        Restriction restriction = mock(Restriction.class);

        // 여러 테스트에서 공통으로 사용되나 일부 테스트에서는 호출되지 않을 수 있으므로 lenient() 적용
        lenient().when(restriction.getRestrictionId()).thenReturn(1L);
        lenient().when(restriction.getUserId()).thenReturn(userId);
        lenient().when(restriction.getDomainType()).thenReturn(RestrictionDomainType.LETTER);
        lenient().when(restriction.getReason()).thenReturn(ReportReason.ABUSE);
        lenient().when(restriction.getDescription()).thenReturn("테스트 제재 사유");
        lenient().when(restriction.getStatus()).thenReturn(status);
        lenient().when(restriction.getCreatedAt()).thenReturn(LocalDateTime.now());
        lenient().when(restriction.getRestrictionUntil()).thenReturn(LocalDateTime.now().plusDays(7));

        return restriction;
    }

    private RestrictionItemResDto mockItemResDto() {
        return RestrictionItemResDto.builder()
                .restrictionId(1L)
                .domainType(RestrictionDomainType.LETTER)
                .reason(ReportReason.ABUSE)
                .description("테스트 제재 사유")
                .restrictionStatus(RestrictionStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .restrictionUntil(LocalDateTime.now().plusDays(7))
                .build();
    }

    // =========================================================================
    // cancelRestriction
    // =========================================================================
    @Nested
    class CancelRestrictionTest {

        @Test
        @DisplayName("[실패] 존재하지 않는 제재 ID이면 RESTRICTION_NOT_FOUND 예외 발생")
        void failWhenRestrictionNotFound() {
            // ============ Given =================
            Long restrictionId = 999L;
            given(restrictionRepository.findById(restrictionId)).willReturn(Optional.empty());

            // ============ When & Then =================
            assertThatThrownBy(() -> adminRestrictionService.cancelRestriction(restrictionId))
                    .isInstanceOf(CustomException.class);
        }

        @Test
        @DisplayName("[실패] 제재 상태가 CANCELED이면 ALREADY_CANCELED 예외 발생")
        void failWhenAlreadyCanceled() {
            // ============ Given =================
            Long restrictionId = 1L;
            Restriction mockRestriction = mockRestriction(2L, RestrictionStatus.CANCELED);
            given(restrictionRepository.findById(restrictionId)).willReturn(Optional.of(mockRestriction));

            // ============ When & Then =================
            assertThatThrownBy(() -> adminRestrictionService.cancelRestriction(restrictionId))
                    .isInstanceOf(CustomException.class);

            verify(mockRestriction, never()).cancel();
        }

        @Test
        @DisplayName("[실패] 제재 상태가 EXPIRED이면 ALREADY_EXPIRED 예외 발생")
        void failWhenAlreadyExpired() {
            // ============ Given =================
            Long restrictionId = 1L;
            Restriction mockRestriction = mockRestriction(2L, RestrictionStatus.EXPIRED);
            given(restrictionRepository.findById(restrictionId)).willReturn(Optional.of(mockRestriction));

            // ============ When & Then =================
            assertThatThrownBy(() -> adminRestrictionService.cancelRestriction(restrictionId))
                    .isInstanceOf(CustomException.class);

            verify(mockRestriction, never()).cancel();
        }

        @Test
        @DisplayName("[실패] 제재 대상 사용자가 존재하지 않으면 USER_NOT_FOUND 예외 발생")
        void failWhenUserNotFound() {
            // ============ Given =================
            Long restrictionId = 1L;
            Long userId = 2L;
            Restriction mockRestriction = mockRestriction(userId, RestrictionStatus.ACTIVE);

            given(restrictionRepository.findById(restrictionId)).willReturn(Optional.of(mockRestriction));
            given(userRepository.findById(userId)).willReturn(Optional.empty());

            // ============ When & Then =================
            assertThatThrownBy(() -> adminRestrictionService.cancelRestriction(restrictionId))
                    .isInstanceOf(CustomException.class);

            // 사용자를 찾지 못해 예외가 발생하더라도 제재 취소(cancel) 메서드는 먼저 호출되어야 함
            verify(mockRestriction, times(1)).cancel();
        }

        @Test
        @DisplayName("[성공] ACTIVE 상태의 제재를 성공적으로 해제한다")
        void successCancelRestriction() {
            // ============ Given =================
            Long restrictionId = 1L;
            Long userId = 2L;

            Restriction mockRestriction = mockRestriction(userId, RestrictionStatus.ACTIVE);
            com.example.egobook_be.domain.user.entity.User mockUser = mock(com.example.egobook_be.domain.user.entity.User.class);
            com.example.egobook_be.domain.restriction.dto.RestrictionCancelResDto mockResDto =
                    mock(com.example.egobook_be.domain.restriction.dto.RestrictionCancelResDto.class);

            given(restrictionRepository.findById(restrictionId)).willReturn(Optional.of(mockRestriction));
            given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
            given(restrictionMapper.toCancelResDto(mockRestriction, mockUser)).willReturn(mockResDto);

            // ============ When =================
            com.example.egobook_be.domain.restriction.dto.RestrictionCancelResDto result =
                    adminRestrictionService.cancelRestriction(restrictionId);

            // ============ Then =================
            assertThat(result).isNotNull();
            assertThat(result).isEqualTo(mockResDto);

            // 검증: 제재 취소 메서드가 정확히 1번 호출되었는가?
            verify(mockRestriction, times(1)).cancel();

            // 검증: 매퍼가 올바른 파라미터로 호출되었는가?
            verify(restrictionMapper, times(1)).toCancelResDto(mockRestriction, mockUser);
        }
    }
}
