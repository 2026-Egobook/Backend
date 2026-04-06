package com.example.egobook_be.domain.user.service;

import com.example.egobook_be.domain.user.dto.SearchUserResDto;
import com.example.egobook_be.domain.user.enums.UserStatus;
import com.example.egobook_be.domain.user.exception.AdminUserErrorCode;
import com.example.egobook_be.domain.user.repository.UserRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class AdminUserServiceUnitTest {

    @InjectMocks
    private AdminUserService adminUserService;

    @Mock
    private UserRepository userRepository;

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

            // Mock 데이터 및 Repository 반환값 설정
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
            // .hasMessageContaining(AdminUserErrorCode.KEYWORD_IS_NULL_OR_BLANK.getMessage()); (필요시 추가)

            assertThatThrownBy(() -> adminUserService.searchUserList(nullKeyword, status, 1, 5))
                    .isInstanceOf(CustomException.class);
        }

        @Test
        @DisplayName("[성공] 페이지 번호, size가 null이면 기본값(page=0, size=5)으로 Pageable이 생성됨")
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
            assertThat(captured.getPageSize()).isEqualTo(5);   // DEFAULT_PAGE_SIZE
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
            assertThat(captured.getPageSize()).isEqualTo(5);   // 2 -> DEFAULT_PAGE_SIZE
        }
    }
}