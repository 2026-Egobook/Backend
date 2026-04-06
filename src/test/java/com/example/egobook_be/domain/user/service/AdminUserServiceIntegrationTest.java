package com.example.egobook_be.domain.user.service;

import com.example.egobook_be.domain.user.dto.SearchUserResDto;
import com.example.egobook_be.domain.user.entity.User;
import com.example.egobook_be.domain.user.enums.UserStatus;
import com.example.egobook_be.domain.user.repository.UserRepository;
import com.example.egobook_be.global.response.SliceResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
public class AdminUserServiceIntegrationTest {

    @Autowired
    private AdminUserService adminUserService;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        // 테스트용 데이터 세팅 (실제 User 엔티티의 생성자나 Builder 구조에 맞게 수정 필요)
        User user1 = User.builder().accountCode("EG001").email("test1@gmail.com").nickname("관리자").status(UserStatus.ACTIVE).build();
        User user2 = User.builder().accountCode("EG002").email("hello@naver.com").nickname("에고북1001").status(UserStatus.ACTIVE).build();
        User user3 = User.builder().accountCode("EG003").email("test2@gmail.com").nickname("에고북1002").status(UserStatus.DORMANT).build();
        User user4 = User.builder().accountCode("EG004").email("java@gmail.com").nickname("에고북0003").status(UserStatus.ACTIVE).build();
        User user5 = User.builder().accountCode("EG005").email("spring@daum.net").nickname("에고북1004").status(UserStatus.ACTIVE).build();
        User user6 = User.builder().accountCode("EG006").email("test3@gmail.com").nickname("에고북1005").status(UserStatus.ACTIVE).build();

        userRepository.saveAll(List.of(user1, user2, user3, user4, user5, user6));
    }

    @Test
    @DisplayName("[통합] 이메일 키워드와 상태 필터로 정확한 데이터를 조회한다")
    void searchUserList_ByEmailAndStatus() {
        // ============ Given =================
        String keyword = "gmail"; // user1, user3, user4, user6 포함됨
        UserStatus status = UserStatus.ACTIVE; // user1, user4, user6만 해당됨 (user3은 DORMANT)
        Integer page = 1;
        Integer size = 10;

        // ============ When =================
        SliceResponse<SearchUserResDto> result = adminUserService.searchUserList(keyword, status, page, size);

        // ============ Then =================
        assertThat(result.content()).hasSize(3);
        assertThat(result.content())
                .extracting("email")
                .containsExactlyInAnyOrder("test1@gmail.com", "java@gmail.com", "test3@gmail.com");
        assertThat(result.hasNext()).isFalse(); // 3개 조회했고 사이즈가 10이므로 다음 페이지 없음
    }

    @Test
    @DisplayName("[통합] 닉네임 키워드로 부분 검색이 정상 동작한다")
    void searchUserList_ByNickname() {
        // ============ Given =================
        String keyword = "에고북1";
        UserStatus status = UserStatus.ACTIVE;
        Integer page = 1;
        Integer size = 5;

        // ============ When =================
        SliceResponse<SearchUserResDto> result = adminUserService.searchUserList(keyword, status, page, size);

        // ============ Then =================
        assertThat(result.content()).hasSize(3);
        assertThat(result.content())
                .extracting("nickname")
                .contains("에고북1001", "에고북1004", "에고북1005");
    }

    @Test
    @DisplayName("[통합] 데이터가 요청 사이즈보다 많을 경우 hasNext가 true로 반환된다")
    void searchUserList_Pagination_HasNextTrue() {
        // ============ Given =================
        String keyword = "EG"; // 모든 유저(6명)의 accountCode에 포함됨
        UserStatus status = UserStatus.ACTIVE; // 5명 해당 (user3 제외)
        Integer page = 1;
        Integer size = 3; // 5명 중 3명만 먼저 조회 요청

        // ============ When =================
        SliceResponse<SearchUserResDto> result = adminUserService.searchUserList(keyword, status, page, size);

        // ============ Then =================
        assertThat(result.content()).hasSize(3); // 첫 페이지 데이터 3건
        assertThat(result.page()).isEqualTo(1);
        assertThat(result.size()).isEqualTo(3);
        assertThat(result.hasNext()).isTrue(); // 남은 데이터가 있으므로 true
    }

    @Test
    @DisplayName("[통합] 다음 페이지(page=2)를 요청하면 나머지 데이터를 반환한다")
    void searchUserList_Pagination_SecondPage() {
        // ============ Given =================
        String keyword = "EG";
        UserStatus status = UserStatus.ACTIVE; // 총 5명
        Integer page = 2; // 두 번째 페이지 요청
        Integer size = 3;

        // ============ When =================
        SliceResponse<SearchUserResDto> result = adminUserService.searchUserList(keyword, status, page, size);

        // ============ Then =================
        assertThat(result.content()).hasSize(2); // 5명 중 1페이지(3명)를 제외한 나머지 2건
        assertThat(result.page()).isEqualTo(2);
        assertThat(result.hasNext()).isFalse(); // 더 이상 남은 데이터가 없으므로 false
    }
}