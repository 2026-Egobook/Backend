package com.example.egobook_be.global.schedular;

import com.example.egobook_be.domain.auth.entity.AuthAccount;
import com.example.egobook_be.domain.auth.enums.Provider;
import com.example.egobook_be.domain.auth.repository.AuthAccountRepository;
import com.example.egobook_be.domain.shop.entity.Item;
import com.example.egobook_be.domain.shop.entity.UserItem;
import com.example.egobook_be.domain.shop.enums.ItemCategory;
import com.example.egobook_be.domain.shop.repository.ItemRepository;
import com.example.egobook_be.domain.user.entity.User;
import com.example.egobook_be.domain.user.enums.UserStatus;
import com.example.egobook_be.domain.user.repository.UserRepository;
import com.example.egobook_be.global.scheduler.DailySchedular;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@ActiveProfiles("local") // application-local.yml 설정을 사용한다고 가정
class DailySchedulerTest {

    @Autowired
    private DailySchedular dailyScheduler;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthAccountRepository authAccountRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Test
    @DisplayName("탈퇴 대기 기간(purgeAt)이 지난 유저는 삭제되고, 아직 남은 유저는 유지된다. 연관 데이터(Cascade)도 함께 삭제되어야 한다.")
    void purgeUsers_IntegrationTest() {
        // =================================================================
        // [Given] 테스트 데이터 세팅
        // =================================================================

        // 1. 삭제 대상 유저 (Target User)
        // - 상태: WITHDRAW_PENDING
        // - purgeAt: 어제 (현재 시간보다 과거)
        User deleteTargetUser = User.builder()
                .accountCode("DELETE_ME")
                .nickname("삭제될유저")
                .status(UserStatus.WITHDRAW_PENDING)
                .purgeAt(LocalDateTime.now().minusDays(1)) // 어제 날짜
                .build();

        // 2. 삭제 대상의 연관 데이터 (Cascade 검증용)
        // (1) AuthAccount 연결
        AuthAccount authAccount = AuthAccount.builder()
                .provider(Provider.GUEST)
                .hashedDeviceUid("dummy_hash")
                .user(deleteTargetUser)
                .build();

        // (2) UserItem 연결 (UserItem 생성을 위해 임시 Item 필요)
        Item dummyItem = itemRepository.save(Item.builder()
                .name("TestItem")
                .price(100)
                .category(ItemCategory.SKIN)
                .path("dummy_image.png")
                .build());
        UserItem userItem = UserItem.builder()
                .user(deleteTargetUser)
                .item(dummyItem)
                .isEquipped(true)
                .build();

        // 여기서는 CascadeType.ALL이 걸려있다고 가정하고 User에 추가
        deleteTargetUser.getUserItems().add(userItem);

        // 부모(User)를 저장하면 자식(AuthAccount, UserItem)도 같이 저장됨 (Cascade Persist)
        // *주의: AuthAccount는 User 필드에 없으면 별도로 저장 필요할 수 있음. 엔티티 구조에 따라 조정.
        userRepository.save(deleteTargetUser);
        authAccountRepository.save(authAccount); // 명시적 저장 (User쪽에서 mappedBy인 경우)


        // 3. 유지 대상 유저 (Survivor User)
        // - 상태: WITHDRAW_PENDING
        // - purgeAt: 내일 (현재 시간보다 미래)
        User survivorUser = User.builder()
                .accountCode("SAVE_ME")
                .nickname("살아남을유저")
                .status(UserStatus.WITHDRAW_PENDING)
                .purgeAt(LocalDateTime.now().plusDays(1)) // 내일 날짜
                .build();
        userRepository.save(survivorUser);

        // 4. 일반 활성 유저 (Active User)
        // - 상태: ACTIVE
        User activeUser = User.builder()
                .accountCode("ACTIVE")
                .nickname("활동중유저")
                .status(UserStatus.ACTIVE)
                .build();
        userRepository.save(activeUser);


        // =================================================================
        // [When] 스케줄러 실행
        // =================================================================
        dailyScheduler.purgeUsers();


        // =================================================================
        // [Then] 검증
        // =================================================================

        // 1. 삭제 대상 유저가 DB에서 사라졌는지 확인
        Optional<User> deletedUserSearch = userRepository.findById(deleteTargetUser.getId());
        assertThat(deletedUserSearch).isEmpty();

        // 2. 연관된 자식 데이터(Cascade)도 실제로 삭제되었는지 확인
        // AuthAccount 조회
        boolean authAccountExists = authAccountRepository.existsById(authAccount.getId());
        assertThat(authAccountExists).isFalse();

        // UserItem 조회 (Item 자체는 지워지면 안 되고, UserItem 매핑만 지워져야 함)
        // UserItem Repository가 있다면 조회, 없다면 로우 쿼리나 EntityManager로 확인 가능
        // 여기서는 UserItemRepository가 없다고 가정하고 Item은 그대로 있는지 확인
        assertThat(itemRepository.existsById(dummyItem.getId())).isTrue();


        // 3. 유지 대상 유저는 살아있는지 확인 (날짜가 아직 안 됨)
        Optional<User> survivorSearch = userRepository.findById(survivorUser.getId());
        assertThat(survivorSearch).isPresent();

        // 4. 활성 유저는 살아있는지 확인 (상태가 PENDING이 아님)
        Optional<User> activeUserSearch = userRepository.findById(activeUser.getId());
        assertThat(activeUserSearch).isPresent();
    }
}
