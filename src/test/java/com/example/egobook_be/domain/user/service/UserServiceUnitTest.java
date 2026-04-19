package com.example.egobook_be.domain.user.service;

import com.example.egobook_be.domain.auth.entity.AuthAccount;
import com.example.egobook_be.domain.auth.entity.RefreshTokenBackup;
import com.example.egobook_be.domain.auth.enums.AuthErrorCode;
import com.example.egobook_be.domain.auth.repository.AuthAccountRepository;
import com.example.egobook_be.domain.auth.repository.RefreshTokenBackupRepository;
import com.example.egobook_be.domain.user.dto.WithdrawReasonReqDto;
import com.example.egobook_be.domain.user.entity.User;
import com.example.egobook_be.domain.user.entity.WithdrawReason;
import com.example.egobook_be.domain.user.exception.UserErrorCode;
import com.example.egobook_be.domain.user.enums.UserStatus;
import com.example.egobook_be.domain.user.enums.WithdrawReasonType;
import com.example.egobook_be.domain.user.repository.UserRepository;
import com.example.egobook_be.domain.user.repository.WithdrawReasonRepository;
import com.example.egobook_be.global.exception.CustomException;
import com.example.egobook_be.global.util.RedisUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class UserServiceUnitTest {

    @InjectMocks
    private UserService userService;

    @Mock private UserRepository userRepository;
    @Mock private AuthAccountRepository authAccountRepository;
    @Mock private RefreshTokenBackupRepository refreshTokenBackupRepository;
    @Mock private WithdrawReasonRepository withdrawReasonRepository;
    @Mock private RedisUtil redisUtil;

    @BeforeEach
    void setUp() {
        // @Value 필드 수동 주입 (예: 30일 = 2592000000L)
        ReflectionTestUtils.setField(userService, "purgeDurationInMs", 2592000000L);
    }

    @Nested
    @DisplayName("withDrawAccount() 회원 탈퇴 메서드 테스트")
    class WithdrawAccountTest {

        @Test
        @DisplayName("[성공] 정상적인 회원 탈퇴 요청 (상태 변경 및 토큰 무효화)")
        void successWithDrawAccount() {
            // ========= Given =========
            Long userId = 1L;
            String rawAccessToken = "valid.access.token";
            String accessTokenWithBearer = "Bearer " + rawAccessToken;

            // 1. User, AuthAccount 엔티티 생성
            User mockUser = User.builder()
                    .id(1L)
                    .status(UserStatus.ACTIVE) // 정상 유저
                    .notificationEnabled(true)
                    .dailyPraise(true)
                    .build();

            AuthAccount realAuthAccount = AuthAccount.builder()
                    .user(mockUser)
                    .build();

            // 2. 삭제할 기존 Refresh Token 백업본 2개 생성
            RefreshTokenBackup backup1 = RefreshTokenBackup.builder().hashedTokenValue("hashed-token-1").build();
            RefreshTokenBackup backup2 = RefreshTokenBackup.builder().hashedTokenValue("hashed-token-2").build();
            List<RefreshTokenBackup> backups = List.of(backup1, backup2);

            // 3. Stub 설정
            // findByIdWithLock으로 비관적 락 조회
            given(userRepository.findByIdWithLock(userId)).willReturn(Optional.of(mockUser));
            given(authAccountRepository.findByUser(mockUser)).willReturn(Optional.of(realAuthAccount));
            given(refreshTokenBackupRepository.findAllByAuthAccount(realAuthAccount)).willReturn(backups);

            // ========= When =========
            userService.withDrawAccount(userId, accessTokenWithBearer);

            // ========= Then =========
            // 1. 엔티티 상태 변경 검증 (State Verification)
            assertThat(mockUser.getStatus())
                    .as("유저 상태가 탈퇴 대기(WITHDRAW_PENDING)로 변경되어야 합니다.")
                    .isEqualTo(UserStatus.WITHDRAW_PENDING);

            // withdrawUser 메서드 내부의 세부 비식별화 동작 검증
            // (참고: User 클래스의 withdrawUser 내부 로직에 맞게 검증, 보통 false나 null로 바뀜)
            assertThat(mockUser.getDeletedAt()).isNotNull();
            assertThat(mockUser.getPurgeAt()).isNotNull();

            // 2. Redis 호출 검증
            verify(redisUtil, times(1)).setTokenInBlacklist(rawAccessToken);

            // 2-2. 백업본 개수(2개)만큼 Redis 삭제 로직이 호출되었는지 확인
            verify(redisUtil, times(1)).deleteHashedRefreshToken("hashed-token-1");
            verify(redisUtil, times(1)).deleteHashedRefreshToken("hashed-token-2");
        }

        @Test
        @DisplayName("[실패 1] 존재하지 않는 사용자의 탈퇴 요청")
        void failUserNotFound() {
            // ========= Given =========
            Long invalidUserId = 999L;
            String accessToken = "Bearer valid.access.token";

            // DB에서 유저를 찾지 못함
            given(userRepository.findByIdWithLock(invalidUserId)).willReturn(Optional.empty());

            // ========= When & Then =========
            CustomException exception = assertThrows(CustomException.class, () -> {
                userService.withDrawAccount(invalidUserId, accessToken);
            });

            assertThat(exception.getErrorCode()).isEqualTo(UserErrorCode.USER_NOT_FOUND);

            // 실패 이후의 로직(AuthAccount 조회, Redis 조작)이 전혀 실행되지 않았음을 검증
            verify(authAccountRepository, never()).findByUser(any());
            verify(redisUtil, never()).setTokenInBlacklist(anyString());
        }

        @Test
        @DisplayName("[실패 2] 사용자의 AuthAccount 인증 정보가 없는 경우")
        void failAuthAccountNotFound() {
            // ========= Given =========
            Long userId = 1L;
            String accessToken = "Bearer valid.access.token";

            User mockUser = User.builder().status(UserStatus.ACTIVE).build();

            given(userRepository.findByIdWithLock(userId)).willReturn(Optional.of(mockUser));
            // AuthAccount를 찾지 못함
            given(authAccountRepository.findByUser(mockUser)).willReturn(Optional.empty());

            // ========= When & Then =========
            CustomException exception = assertThrows(CustomException.class, () -> {
                userService.withDrawAccount(userId, accessToken);
            });

            assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.USER_AUTH_ACCOUNT_NOT_FOUND);

            // 실패 이후의 Redis 조작 로직이 전혀 실행되지 않았음을 검증
            verify(redisUtil, never()).setTokenInBlacklist(anyString());
        }

        @Test
        @DisplayName("[실패 3] 이미 탈퇴 대기(WITHDRAW_PENDING) 상태인 사용자가 다시 탈퇴 요청")
        void failAlreadyWithdrawPending() {
            // ========= Given =========
            Long userId = 1L;
            String accessToken = "Bearer valid.access.token";

            // 이미 탈퇴 대기 상태인 유저 생성
            User mockUser = User.builder().status(UserStatus.WITHDRAW_PENDING).build();
            AuthAccount realAuthAccount = AuthAccount.builder().user(mockUser).build();

            given(userRepository.findByIdWithLock(userId)).willReturn(Optional.of(mockUser));
            given(authAccountRepository.findByUser(mockUser)).willReturn(Optional.of(realAuthAccount));

            // ========= When & Then =========
            CustomException exception = assertThrows(CustomException.class, () -> {
                userService.withDrawAccount(userId, accessToken);
            });

            // 멱등성 보장을 위한 커스텀 예외 확인
            assertThat(exception.getErrorCode()).isEqualTo(UserErrorCode.ALREADY_WITHDRAW_PENDING);

            // 실패 이후의 토큰 무효화 로직이 실행되지 않았음을 검증
            verify(refreshTokenBackupRepository, never()).findAllByAuthAccount(any());
            verify(redisUtil, never()).setTokenInBlacklist(anyString());
        }
    }

    @Nested
    @DisplayName("saveWithdrawReason() 탈퇴 정보 저장 메서드 테스트")
    class WithdrawReasonTest {

        @Test
        @DisplayName("[성공] 탈퇴 정보 저장 성공")
        void saveWithdrawReason() {
            // ========= Given =========
            Long userId = 1L;
            WithdrawReasonReqDto reqDto = new WithdrawReasonReqDto(WithdrawReasonType.LACK_OF_CONTENT, "콘텐츠가 부족해요");

            // 1. 유저가 존재함
            given(userRepository.existsById(userId)).willReturn(true);
            // 2. 이전에 탈퇴 사유를 작성한 적이 없음
            given(withdrawReasonRepository.existsByUserId(userId)).willReturn(false);

            // ========= When =========
            userService.saveWithdrawReason(userId, reqDto);

            // ========= Then =========
            // save 메서드가 정확히 1번 호출되었는지 검증
            verify(withdrawReasonRepository, times(1)).save(any(WithdrawReason.class));
        }

        @Test
        @DisplayName("[실패 1] 이미 존재하는 사용자의 탈퇴 정보 저장 시도")
        void failAlreadyExistUserWithdrawReason() {
            // ========= Given =========
            Long userId = 1L;
            WithdrawReasonReqDto reqDto = new WithdrawReasonReqDto(WithdrawReasonType.NOT_USED_OFTEN, null);

            given(userRepository.existsById(userId)).willReturn(true);
            // 이미 탈퇴 사유를 작성한 적이 있음 (true 반환)
            given(withdrawReasonRepository.existsByUserId(userId)).willReturn(true);

            // ========= When & Then =========
            CustomException exception = assertThrows(CustomException.class, () -> {
                userService.saveWithdrawReason(userId, reqDto);
            });

            // 올바른 예외 코드가 반환되었는지 확인
            assertThat(exception.getErrorCode()).isEqualTo(UserErrorCode.WITHDRAW_REASON_ALREADY_WRITTEN);

            // save가 호출되지 않고 로직이 중단되었는지 확인
            verify(withdrawReasonRepository, never()).save(any());
        }

        @Test
        @DisplayName("[실패 2] 기타(OTHER) 선택 후 상세 내용을 빈칸으로 보냄")
        void failSelectOtherAndNoText() {
            // ========= Given =========
            Long userId = 1L;
            // OTHER를 선택했지만 text를 공백("   ")으로 보냄 (isBlank()에 걸림)
            WithdrawReasonReqDto reqDto = new WithdrawReasonReqDto(WithdrawReasonType.OTHER, "   ");

            given(userRepository.existsById(userId)).willReturn(true);
            given(withdrawReasonRepository.existsByUserId(userId)).willReturn(false);

            // ========= When & Then =========
            CustomException exception = assertThrows(CustomException.class, () -> {
                userService.saveWithdrawReason(userId, reqDto);
            });

            assertThat(exception.getErrorCode()).isEqualTo(UserErrorCode.WITHDRAW_REASON_OTHER_TEXT_FIELD_EMPTY);
            verify(withdrawReasonRepository, never()).save(any());
        }

        @Test
        @DisplayName("[실패 3] 존재하지 않는 사용자의 탈퇴 정보 저장 시도")
        void failUserNotFound() {
            // ========= Given =========
            Long invalidUserId = 999L;
            WithdrawReasonReqDto reqDto = new WithdrawReasonReqDto(WithdrawReasonType.LACK_OF_CONTENT, null);

            // DB에서 유저를 찾지 못함 (false 반환)
            given(userRepository.existsById(invalidUserId)).willReturn(false);

            // ========= When & Then =========
            CustomException exception = assertThrows(CustomException.class, () -> {
                userService.saveWithdrawReason(invalidUserId, reqDto);
            });

            assertThat(exception.getErrorCode()).isEqualTo(UserErrorCode.USER_NOT_FOUND);

            // 이후 로직(존재 여부 확인 및 저장)이 실행되지 않았는지 검증
            verify(withdrawReasonRepository, never()).existsByUserId(any());
            verify(withdrawReasonRepository, never()).save(any());
        }
    }
}