package com.example.egobook_be.domain.user.service;

import com.example.egobook_be.domain.auth.entity.AuthAccount;
import com.example.egobook_be.domain.auth.repository.AuthAccountRepository;
import com.example.egobook_be.domain.diary.repository.DiaryRepository;
import com.example.egobook_be.domain.letters.repository.PlazaLetterReplyReportRepository;
import com.example.egobook_be.domain.letters.repository.PlazaLetterReplyRepository;
import com.example.egobook_be.domain.letters.repository.PlazaLetterReportRepository;
import com.example.egobook_be.domain.letters.repository.PlazaLetterRepository;
import com.example.egobook_be.domain.question.repository.AnswerReportRepository;
import com.example.egobook_be.domain.question.repository.QuestionAnswerRepository;
import com.example.egobook_be.domain.user.dto.AdminUserInfoResDto;
import com.example.egobook_be.domain.user.dto.AdminUserReportHistoryResDto;
import com.example.egobook_be.domain.user.dto.AdminUserStatsResDto;
import com.example.egobook_be.domain.user.dto.SearchUserResDto;
import com.example.egobook_be.domain.user.entity.Ability;
import com.example.egobook_be.domain.user.entity.User;
import com.example.egobook_be.domain.user.enums.UserStatus;
import com.example.egobook_be.domain.restriction.repository.RestrictionRepository;
import com.example.egobook_be.domain.user.mapper.AdminUserMapper;
import com.example.egobook_be.domain.user.repository.AbilityRepository;
import com.example.egobook_be.domain.user.repository.UserRepository;
import com.example.egobook_be.global.enums.ReportDomainType;
import com.example.egobook_be.global.enums.ReportType;
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

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AdminUserServiceUnitTest {

    @InjectMocks
    private AdminUserService adminUserService;

    @Mock private UserRepository userRepository;
    @Mock private AuthAccountRepository authAccountRepository;
    @Mock private AbilityRepository abilityRepository;
    @Mock private DiaryRepository diaryRepository;
    @Mock private PlazaLetterRepository plazaLetterRepository;
    @Mock private PlazaLetterReplyRepository plazaLetterReplyRepository;
    @Mock private QuestionAnswerRepository questionAnswerRepository;
    @Mock private PlazaLetterReportRepository plazaLetterReportRepository;
    @Mock private PlazaLetterReplyReportRepository plazaLetterReplyReportRepository;
    @Mock private AnswerReportRepository answerReportRepository;
    @Mock private AdminUserMapper adminUserMapper;
    @Mock private RestrictionRepository restrictionRepository;

    // =========================================================================
    // searchUserList
    // =========================================================================
    @Nested
    class SearchUserListTest {

        @Test
        @DisplayName("[성공] 사용자 정보 리스트 조회 성공")
        void successSearchUserList() {
            // ============ Given =================
            String keyword = "test@example.com";
            UserStatus status = UserStatus.ACTIVE;
            Integer page = 2;
            Integer size = 7;

            SearchUserResDto mockDto = new SearchUserResDto(1L, "CODE123", "test@example.com", "닉네임", UserStatus.ACTIVE);
            Slice<SearchUserResDto> mockSlice = new SliceImpl<>(List.of(mockDto));

            given(userRepository.findUsersByKeywordAndStatus(eq(keyword), eq(status), any(Pageable.class)))
                    .willReturn(mockSlice);

            // ============ When =================
            SliceResponse<SearchUserResDto> result = adminUserService.searchUserList(keyword, status, page, size);

            // ============ Then =================
            assertThat(result).isNotNull();

            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
            verify(userRepository).findUsersByKeywordAndStatus(eq(keyword), eq(status), pageableCaptor.capture());

            Pageable captured = pageableCaptor.getValue();
            assertThat(captured.getPageNumber()).isEqualTo(1); // 2 - 1 = 1
            assertThat(captured.getPageSize()).isEqualTo(7);
            assertThat(captured.getSort().getOrderFor("id").getDirection()).isEqualTo(Sort.Direction.ASC);
        }

        @Test
        @DisplayName("[실패] 입력된 Keyword가 빈칸이거나 null인 경우")
        void failKeywordIsBlank() {
            // ============ Given =================
            String blankKeyword = "   ";
            String nullKeyword = null;
            UserStatus status = UserStatus.ACTIVE;

            // ============ When =================
            // ============ Then =================
            assertThatThrownBy(() -> adminUserService.searchUserList(blankKeyword, status, 1, 5))
                    .isInstanceOf(CustomException.class);

            assertThatThrownBy(() -> adminUserService.searchUserList(nullKeyword, status, 1, 5))
                    .isInstanceOf(CustomException.class);
        }

        @Test
        @DisplayName("[성공] 페이지 번호, size가 null이면 기본값(page=0, size=1)으로 Pageable이 생성됨")
        void successSearchUserListWithNullPageAndSize() {
            // ============ Given =================
            String keyword = "test";
            UserStatus status = UserStatus.ACTIVE;

            given(userRepository.findUsersByKeywordAndStatus(eq(keyword), eq(status), any(Pageable.class)))
                    .willReturn(new SliceImpl<>(Collections.emptyList()));

            // ============ When =================
            adminUserService.searchUserList(keyword, status, null, null);

            // ============ Then =================
            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
            verify(userRepository).findUsersByKeywordAndStatus(eq(keyword), eq(status), pageableCaptor.capture());

            Pageable captured = pageableCaptor.getValue();
            assertThat(captured.getPageNumber()).isEqualTo(0); // null -> 1 -> (1 - 1) = 0
            assertThat(captured.getPageSize()).isEqualTo(1);   // MIN_PAGE_SIZE
        }

        @Test
        @DisplayName("[성공] 요청 사이즈가 MAX_PAGE_SIZE(10)를 초과하면 10으로 강제 조정됨")
        void successSearchUserListCapsSizeLimit() {
            // ============ Given =================
            String keyword = "test";
            UserStatus status = UserStatus.ACTIVE;
            Integer page = 1;
            Integer size = 50; // 최대 허용치 초과

            given(userRepository.findUsersByKeywordAndStatus(eq(keyword), eq(status), any(Pageable.class)))
                    .willReturn(new SliceImpl<>(Collections.emptyList()));

            // ============ When =================
            adminUserService.searchUserList(keyword, status, page, size);

            // ============ Then =================
            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
            verify(userRepository).findUsersByKeywordAndStatus(eq(keyword), eq(status), pageableCaptor.capture());

            assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(10); // MAX_PAGE_SIZE
        }

        @Test
        @DisplayName("[성공] 비정상적인 범위의 page(<1)와 size(<5)가 입력되면 최소값으로 조정됨")
        void successSearchUserListAdjustsInvalidPageAndSize() {
            // ============ Given =================
            String keyword = "test";
            UserStatus status = UserStatus.ACTIVE;
            Integer invalidPage = 0; // 1보다 작은 값
            Integer invalidSize = 2; // DEFAULT_PAGE_SIZE(5) 보다 작은 값

            given(userRepository.findUsersByKeywordAndStatus(eq(keyword), eq(status), any(Pageable.class)))
                    .willReturn(new SliceImpl<>(Collections.emptyList()));

            // ============ When =================
            adminUserService.searchUserList(keyword, status, invalidPage, invalidSize);

            // ============ Then =================
            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
            verify(userRepository).findUsersByKeywordAndStatus(eq(keyword), eq(status), pageableCaptor.capture());

            Pageable captured = pageableCaptor.getValue();
            assertThat(captured.getPageNumber()).isEqualTo(0); // 0 -> 1 -> (1 - 1) = 0
            assertThat(captured.getPageSize()).isEqualTo(2);   // 1 -> MIN_PAGE_SIZE
        }
    }

    // =========================================================================
    // getUserInfo
    // =========================================================================
    @Nested
    class GetUserInfoTest {

        @Test
        @DisplayName("[성공] 사용자 기본 정보 조회 성공")
        void successGetUserInfo() {
            // ============ Given =================
            Long userId = 1L;
            User mockUser = mock(User.class);
            AuthAccount mockAuthAccount = mock(AuthAccount.class);
            AdminUserInfoResDto expectedDto = mock(AdminUserInfoResDto.class);

            given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
            given(authAccountRepository.findByUser(mockUser)).willReturn(Optional.of(mockAuthAccount));
            given(adminUserMapper.toAdminUserInfoResDto(mockUser, mockAuthAccount)).willReturn(expectedDto);

            // ============ When =================
            AdminUserInfoResDto result = adminUserService.getUserInfo(userId);

            // ============ Then =================
            assertThat(result).isNotNull();
            assertThat(result).isEqualTo(expectedDto);
            verify(userRepository).findById(userId);
            verify(authAccountRepository).findByUser(mockUser);
            verify(adminUserMapper).toAdminUserInfoResDto(mockUser, mockAuthAccount);
        }

        @Test
        @DisplayName("[실패] 존재하지 않는 userId이면 CustomException(USER_NOT_FOUND) 발생")
        void failGetUserInfoUserNotFound() {
            // ============ Given =================
            Long invalidUserId = 999L;

            given(userRepository.findById(invalidUserId)).willReturn(Optional.empty());

            // ============ When =================
            // ============ Then =================
            assertThatThrownBy(() -> adminUserService.getUserInfo(invalidUserId))
                    .isInstanceOf(CustomException.class);

            verify(authAccountRepository, never()).findByUser(any());
        }

        @Test
        @DisplayName("[실패] AuthAccount가 없으면 CustomException(AUTH_ACCOUNT_NOT_FOUND) 발생")
        void failGetUserInfoAuthAccountNotFound() {
            // ============ Given =================
            Long userId = 1L;
            User mockUser = mock(User.class);

            given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
            given(authAccountRepository.findByUser(mockUser)).willReturn(Optional.empty());

            // ============ When =================
            // ============ Then =================
            assertThatThrownBy(() -> adminUserService.getUserInfo(userId))
                    .isInstanceOf(CustomException.class);

            verify(adminUserMapper, never()).toAdminUserInfoResDto(any(), any());
        }
    }

    // =========================================================================
    // getUserStats
    // =========================================================================
    @Nested
    class GetUserStatsTest {

        @Test
        @DisplayName("[성공] 사용자 활동 통계 조회 성공 - 각 카운트가 DTO에 올바르게 전달됨")
        void successGetUserStats() {
            // ============ Given =================
            Long userId = 1L;
            User mockUser = mock(User.class);
            Ability mockAbility = mock(Ability.class);
            AdminUserStatsResDto expectedDto = mock(AdminUserStatsResDto.class);

            given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
            given(abilityRepository.findByUser(mockUser)).willReturn(Optional.of(mockAbility));
            given(diaryRepository.countByUser(mockUser)).willReturn(3L);
            given(plazaLetterRepository.countBySenderId(userId)).willReturn(5);
            given(plazaLetterReplyRepository.countByReplierId(userId)).willReturn(7L);
            given(questionAnswerRepository.countByUser(mockUser)).willReturn(2L);
            given(adminUserMapper.toAdminUserStatsResDto(mockUser, mockAbility, 3L, 5L, 7L, 2L))
                    .willReturn(expectedDto);

            // ============ When =================
            AdminUserStatsResDto result = adminUserService.getUserStats(userId);

            // ============ Then =================
            assertThat(result).isNotNull();
            assertThat(result).isEqualTo(expectedDto);
            verify(adminUserMapper).toAdminUserStatsResDto(mockUser, mockAbility, 3L, 5L, 7L, 2L);
        }

        @Test
        @DisplayName("[실패] 존재하지 않는 userId이면 CustomException(USER_NOT_FOUND) 발생")
        void failGetUserStatsUserNotFound() {
            // ============ Given =================
            Long invalidUserId = 999L;

            given(userRepository.findById(invalidUserId)).willReturn(Optional.empty());

            // ============ When =================
            // ============ Then =================
            assertThatThrownBy(() -> adminUserService.getUserStats(invalidUserId))
                    .isInstanceOf(CustomException.class);

            verify(abilityRepository, never()).findByUser(any());
        }

        @Test
        @DisplayName("[실패] Ability가 없으면 CustomException(ABILITY_NOT_FOUND) 발생")
        void failGetUserStatsAbilityNotFound() {
            // ============ Given =================
            Long userId = 1L;
            User mockUser = mock(User.class);

            given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
            given(abilityRepository.findByUser(mockUser)).willReturn(Optional.empty());

            // ============ When =================
            // ============ Then =================
            assertThatThrownBy(() -> adminUserService.getUserStats(userId))
                    .isInstanceOf(CustomException.class);

            verify(diaryRepository, never()).countByUser(any());
        }
    }

    // =========================================================================
    // getUserReportHistory
    // =========================================================================
    @Nested
    class GetUserReportHistoryTest {

        private static final Long USER_ID = 1L;

        @Test
        @DisplayName("[실패] 존재하지 않는 userId이면 CustomException(USER_NOT_FOUND) 발생")
        void failGetUserReportHistoryUserNotFound() {
            // ============ Given =================
            given(userRepository.existsById(USER_ID)).willReturn(false);

            // ============ When =================
            // ============ Then =================
            assertThatThrownBy(() -> adminUserService.getUserReportHistory(
                    USER_ID, ReportDomainType.LETTER, null, null, null, 1, 5))
                    .isInstanceOf(CustomException.class);
        }

        @Test
        @DisplayName("[성공] page/size가 null이면 기본값(page=0, size=1)으로 Pageable이 생성되고 createdAt 기준 내림차순 정렬")
        void successWithNullPageAndSize() {
            // ============ Given =================
            given(userRepository.existsById(USER_ID)).willReturn(true);
            given(plazaLetterReportRepository.countByUserId(eq(USER_ID), any(), any())).willReturn(0L);
            given(plazaLetterReportRepository.findPlazaLetterReportsByUserIdWithoutReportType(
                    eq(USER_ID), any(), any(), any(Pageable.class)))
                    .willReturn(new SliceImpl<>(Collections.emptyList()));
            given(adminUserMapper.toAdminUserReportHistoryResDto(any(), any()))
                    .willReturn(mock(AdminUserReportHistoryResDto.class));

            // ============ When =================
            adminUserService.getUserReportHistory(
                    USER_ID, ReportDomainType.LETTER, null, null, null, null, null);

            // ============ Then =================
            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            verify(plazaLetterReportRepository).findPlazaLetterReportsByUserIdWithoutReportType(
                    eq(USER_ID), any(), any(), captor.capture());

            Pageable captured = captor.getValue();
            assertThat(captured.getPageNumber()).isEqualTo(0);
            assertThat(captured.getSort().getOrderFor("createdAt").getDirection()).isEqualTo(Sort.Direction.DESC);
        }

        @Test
        @DisplayName("[성공] size가 MAX_PAGE_SIZE(10) 초과이면 10으로 강제 조정됨")
        void successCapsSizeLimit() {
            // ============ Given =================
            given(userRepository.existsById(USER_ID)).willReturn(true);
            given(plazaLetterReportRepository.countByUserId(eq(USER_ID), any(), any())).willReturn(0L);
            given(plazaLetterReportRepository.findPlazaLetterReportsByUserIdWithoutReportType(
                    eq(USER_ID), any(), any(), any(Pageable.class)))
                    .willReturn(new SliceImpl<>(Collections.emptyList()));
            given(adminUserMapper.toAdminUserReportHistoryResDto(any(), any()))
                    .willReturn(mock(AdminUserReportHistoryResDto.class));

            // ============ When =================
            adminUserService.getUserReportHistory(
                    USER_ID, ReportDomainType.LETTER, null, null, null, 1, 500);

            // ============ Then =================
            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            verify(plazaLetterReportRepository).findPlazaLetterReportsByUserIdWithoutReportType(
                    eq(USER_ID), any(), any(), captor.capture());

            assertThat(captor.getValue().getPageSize()).isEqualTo(10);
        }

        // ── LETTER ────────────────────────────────────────────────────────
        @Nested
        class LetterDomainTest {

            @Test
            @DisplayName("[성공] reportType=null → countByUserId 호출, 전체 신고 조회")
            void successReportTypeNull() {
                // ============ Given =================
                given(userRepository.existsById(USER_ID)).willReturn(true);
                given(plazaLetterReportRepository.countByUserId(eq(USER_ID), any(), any())).willReturn(4L);
                given(plazaLetterReportRepository.findPlazaLetterReportsByUserIdWithoutReportType(
                        eq(USER_ID), any(), any(), any(Pageable.class)))
                        .willReturn(new SliceImpl<>(Collections.emptyList()));
                AdminUserReportHistoryResDto expectedDto = mock(AdminUserReportHistoryResDto.class);
                given(adminUserMapper.toAdminUserReportHistoryResDto(any(), any())).willReturn(expectedDto);

                // ============ When =================
                AdminUserReportHistoryResDto result = adminUserService.getUserReportHistory(
                        USER_ID, ReportDomainType.LETTER, null, null, null, 1, 5);

                // ============ Then =================
                assertThat(result).isEqualTo(expectedDto);
                verify(plazaLetterReportRepository).countByUserId(eq(USER_ID), any(), any());
                verify(plazaLetterReportRepository, never()).countByReporterId(any(), any(), any());
                verify(plazaLetterReportRepository, never()).countBySenderId(any(), any(), any());
            }

            @Test
            @DisplayName("[성공] reportType=REPORTER → countByReporterId 호출, reporterId 기준으로 신고 목록 조회")
            void successReportTypeReporter() {
                // ============ Given =================
                given(userRepository.existsById(USER_ID)).willReturn(true);
                given(plazaLetterReportRepository.countByReporterId(eq(USER_ID), any(), any())).willReturn(2L);
                given(plazaLetterReportRepository.countBySenderId(eq(USER_ID), any(), any())).willReturn(1L);
                given(plazaLetterReportRepository.findPlazaLetterReportsByReporterId(
                        eq(USER_ID), any(), any(), any(Pageable.class)))
                        .willReturn(new SliceImpl<>(Collections.emptyList()));
                given(adminUserMapper.toAdminUserReportHistoryResDto(any(), any()))
                        .willReturn(mock(AdminUserReportHistoryResDto.class));

                // ============ When =================
                adminUserService.getUserReportHistory(
                        USER_ID, ReportDomainType.LETTER, ReportType.REPORTER, null, null, 1, 5);

                // ============ Then =================
                verify(plazaLetterReportRepository).findPlazaLetterReportsByReporterId(
                        eq(USER_ID), any(), any(), any(Pageable.class));
                verify(plazaLetterReportRepository, never()).findPlazaLetterReportsBySenderId(
                        any(), any(), any(), any());
            }

            @Test
            @DisplayName("[성공] reportType=REPORTED → countBySenderId 호출, senderId 기준으로 신고 목록 조회")
            void successReportTypeReported() {
                // ============ Given =================
                given(userRepository.existsById(USER_ID)).willReturn(true);
                given(plazaLetterReportRepository.countByReporterId(eq(USER_ID), any(), any())).willReturn(0L);
                given(plazaLetterReportRepository.countBySenderId(eq(USER_ID), any(), any())).willReturn(3L);
                given(plazaLetterReportRepository.findPlazaLetterReportsBySenderId(
                        eq(USER_ID), any(), any(), any(Pageable.class)))
                        .willReturn(new SliceImpl<>(Collections.emptyList()));
                given(adminUserMapper.toAdminUserReportHistoryResDto(any(), any()))
                        .willReturn(mock(AdminUserReportHistoryResDto.class));

                // ============ When =================
                adminUserService.getUserReportHistory(
                        USER_ID, ReportDomainType.LETTER, ReportType.REPORTED, null, null, 1, 5);

                // ============ Then =================
                verify(plazaLetterReportRepository).findPlazaLetterReportsBySenderId(
                        eq(USER_ID), any(), any(), any(Pageable.class));
                verify(plazaLetterReportRepository, never()).findPlazaLetterReportsByReporterId(
                        any(), any(), any(), any());
            }
        }

        // ── LETTER_REPLY ───────────────────────────────────────────────────
        @Nested
        class LetterReplyDomainTest {

            @Test
            @DisplayName("[성공] reportType=null → countByUserId 호출, 전체 신고 조회")
            void successReportTypeNull() {
                // ============ Given =================
                given(userRepository.existsById(USER_ID)).willReturn(true);
                given(plazaLetterReplyReportRepository.countByUserId(eq(USER_ID), any(), any())).willReturn(5L);
                given(plazaLetterReplyReportRepository.findPlazaLetterReplyReportsByUserIdWithoutReportType(
                        eq(USER_ID), any(), any(), any(Pageable.class)))
                        .willReturn(new SliceImpl<>(Collections.emptyList()));
                given(adminUserMapper.toAdminUserReportHistoryResDto(any(), any()))
                        .willReturn(mock(AdminUserReportHistoryResDto.class));

                // ============ When =================
                adminUserService.getUserReportHistory(
                        USER_ID, ReportDomainType.LETTER_REPLY, null, null, null, 1, 5);

                // ============ Then =================
                verify(plazaLetterReplyReportRepository).countByUserId(eq(USER_ID), any(), any());
                verify(plazaLetterReplyReportRepository, never()).countByReporterId(any(), any(), any());
                verify(plazaLetterReplyReportRepository, never()).countByReplierId(any(), any(), any());
            }

            @Test
            @DisplayName("[성공] reportType=REPORTER → countByReporterId 호출, reporterId 기준으로 신고 목록 조회")
            void successReportTypeReporter() {
                // ============ Given =================
                given(userRepository.existsById(USER_ID)).willReturn(true);
                given(plazaLetterReplyReportRepository.countByReporterId(eq(USER_ID), any(), any())).willReturn(3L);
                given(plazaLetterReplyReportRepository.countByReplierId(eq(USER_ID), any(), any())).willReturn(1L);
                given(plazaLetterReplyReportRepository.findPlazaLetterReplyReportsByReporterId(
                        eq(USER_ID), any(), any(), any(Pageable.class)))
                        .willReturn(new SliceImpl<>(Collections.emptyList()));
                given(adminUserMapper.toAdminUserReportHistoryResDto(any(), any()))
                        .willReturn(mock(AdminUserReportHistoryResDto.class));

                // ============ When =================
                adminUserService.getUserReportHistory(
                        USER_ID, ReportDomainType.LETTER_REPLY, ReportType.REPORTER, null, null, 1, 5);

                // ============ Then =================
                verify(plazaLetterReplyReportRepository).findPlazaLetterReplyReportsByReporterId(
                        eq(USER_ID), any(), any(), any(Pageable.class));
                verify(plazaLetterReplyReportRepository, never()).findPlazaLetterReplyReportsByReplierId(
                        any(), any(), any(), any());
            }

            @Test
            @DisplayName("[성공] reportType=REPORTED → countByReplierId 호출, replierId 기준으로 신고 목록 조회")
            void successReportTypeReported() {
                // ============ Given =================
                given(userRepository.existsById(USER_ID)).willReturn(true);
                given(plazaLetterReplyReportRepository.countByReporterId(eq(USER_ID), any(), any())).willReturn(0L);
                given(plazaLetterReplyReportRepository.countByReplierId(eq(USER_ID), any(), any())).willReturn(2L);
                given(plazaLetterReplyReportRepository.findPlazaLetterReplyReportsByReplierId(
                        eq(USER_ID), any(), any(), any(Pageable.class)))
                        .willReturn(new SliceImpl<>(Collections.emptyList()));
                given(adminUserMapper.toAdminUserReportHistoryResDto(any(), any()))
                        .willReturn(mock(AdminUserReportHistoryResDto.class));

                // ============ When =================
                adminUserService.getUserReportHistory(
                        USER_ID, ReportDomainType.LETTER_REPLY, ReportType.REPORTED, null, null, 1, 5);

                // ============ Then =================
                verify(plazaLetterReplyReportRepository).findPlazaLetterReplyReportsByReplierId(
                        eq(USER_ID), any(), any(), any(Pageable.class));
                verify(plazaLetterReplyReportRepository, never()).findPlazaLetterReplyReportsByReporterId(
                        any(), any(), any(), any());
            }
        }

        // ── QUESTION_ANSWER ────────────────────────────────────────────────
        @Nested
        class QuestionAnswerDomainTest {

            @Test
            @DisplayName("[성공] reportType=null → countByUserId 호출, 전체 신고 조회")
            void successReportTypeNull() {
                // ============ Given =================
                given(userRepository.existsById(USER_ID)).willReturn(true);
                given(answerReportRepository.countByUserId(eq(USER_ID), any(), any())).willReturn(6L);
                given(answerReportRepository.findAnswerReportsByUserIdWithoutReportType(
                        eq(USER_ID), any(), any(), any(Pageable.class)))
                        .willReturn(new SliceImpl<>(Collections.emptyList()));
                given(adminUserMapper.toAdminUserReportHistoryResDto(any(), any()))
                        .willReturn(mock(AdminUserReportHistoryResDto.class));

                // ============ When =================
                adminUserService.getUserReportHistory(
                        USER_ID, ReportDomainType.QUESTION_ANSWER, null, null, null, 1, 5);

                // ============ Then =================
                verify(answerReportRepository).countByUserId(eq(USER_ID), any(), any());
                verify(answerReportRepository, never()).countByReporterId(any(), any(), any());
                verify(answerReportRepository, never()).countByAnswererId(any(), any(), any());
            }

            @Test
            @DisplayName("[성공] reportType=REPORTER → countByReporterId 호출, 신고한 답변 목록 조회")
            void successReportTypeReporter() {
                // ============ Given =================
                given(userRepository.existsById(USER_ID)).willReturn(true);
                given(answerReportRepository.countByReporterId(eq(USER_ID), any(), any())).willReturn(2L);
                given(answerReportRepository.countByAnswererId(eq(USER_ID), any(), any())).willReturn(1L);

                // REPORTER 분기에서 실제로 호출되는 메서드로 수정
                given(answerReportRepository.findAnswerReportsByReporterId(
                        eq(USER_ID), any(), any(), any(Pageable.class)))
                        .willReturn(new SliceImpl<>(Collections.emptyList()));

                given(adminUserMapper.toAdminUserReportHistoryResDto(any(), any()))
                        .willReturn(mock(AdminUserReportHistoryResDto.class));

                // ============ When =================
                adminUserService.getUserReportHistory(
                        USER_ID, ReportDomainType.QUESTION_ANSWER, ReportType.REPORTER, null, null, 1, 5);

                // ============ Then =================
                verify(answerReportRepository).countByReporterId(eq(USER_ID), any(), any());
                verify(answerReportRepository).findAnswerReportsByReporterId(
                        eq(USER_ID), any(), any(), any(Pageable.class));

                // REPORTED 관련 메서드는 호출되지 않아야 함
                verify(answerReportRepository, never()).findAnswerReportsByAnswererId(
                        any(), any(), any(), any(Pageable.class));
            }

            @Test
            @DisplayName("[성공] reportType=REPORTED → countByAnswererId 호출, 신고받은 답변 목록 조회")
            void successReportTypeReported() {
                // ============ Given =================
                given(userRepository.existsById(USER_ID)).willReturn(true);
                given(answerReportRepository.countByReporterId(eq(USER_ID), any(), any())).willReturn(0L);
                given(answerReportRepository.countByAnswererId(eq(USER_ID), any(), any())).willReturn(4L);
                given(answerReportRepository.findAnswerReportsByAnswererId(
                        eq(USER_ID), any(), any(), any(Pageable.class)))
                        .willReturn(new SliceImpl<>(Collections.emptyList()));
                given(adminUserMapper.toAdminUserReportHistoryResDto(any(), any()))
                        .willReturn(mock(AdminUserReportHistoryResDto.class));

                // ============ When =================
                adminUserService.getUserReportHistory(
                        USER_ID, ReportDomainType.QUESTION_ANSWER, ReportType.REPORTED, null, null, 1, 5);

                // ============ Then =================
                verify(answerReportRepository).findAnswerReportsByAnswererId(
                        eq(USER_ID), any(), any(), any(Pageable.class));
            }
        }
    }
}