package com.example.egobook_be.domain.shop.service;

import com.example.egobook_be.domain.shop.dto.PurchaseItemReqDto;
import com.example.egobook_be.domain.shop.entity.Item;
import com.example.egobook_be.domain.shop.enums.ItemCategory;
import com.example.egobook_be.domain.shop.enums.ShopErrorCode;
import com.example.egobook_be.domain.shop.repository.ItemRepository;
import com.example.egobook_be.domain.shop.repository.UserItemRepository;
import com.example.egobook_be.domain.user.entity.User;
import com.example.egobook_be.domain.user.repository.UserRepository;
import com.example.egobook_be.global.exception.CustomException;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
public class ShopServiceIntegrationTest {
    @Autowired private ShopService shopService;
    @Autowired private UserRepository userRepository;
    @Autowired private ItemRepository itemRepository;
    @Autowired private UserItemRepository userItemRepository;

    private Long userId;
    private Long affordableItemId; // 살 수 있는 아이템 ID
    private Long expensiveItemId;  // 비싸서 못 사는 아이템 ID

    @BeforeEach
    public void setup() {
        // 1. 유저 세팅 (기존 코드 유지)
        User user = User.builder()
                .accountCode("test")
                .email("test@example.com")
                .nickname("testNickname")
                .lastLoginAt(LocalDateTime.now().minusDays(1))
                .ink(10) // 유저 잉크 10 보유
                .build();
        userRepository.save(user);
        this.userId = user.getId();

        // 2. 구매 가능한 저렴한 아이템 세팅 (가격 5)
        Item affordableItem = Item.builder()
                .name("일반 아이템")
                .path("/test")
                .category(ItemCategory.BACK)
                .price(5)
                .build();
        itemRepository.save(affordableItem);
        this.affordableItemId = affordableItem.getId();

        // 3. 구매 불가능한 비싼 아이템 세팅 (가격 50)
        Item expensiveItem = Item.builder()
                .name("비싼 아이템")
                .path("/test")
                .category(ItemCategory.BACK)
                .price(50)
                .build();
        itemRepository.save(expensiveItem);
        this.expensiveItemId = expensiveItem.getId();
    }

    // 수동으로 각 테스트 코드 실행 후 DB 초기화 진행
    @AfterEach
    void tearDown(){
        userItemRepository.deleteAllInBatch();
        itemRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }

    @Nested
    @DisplayName("purchaseItem() 메서드 테스트")
    class PurchaseItemTest{
        @Test
        @DisplayName("[성공] 같은 사용자의 구매 요청이 동시에 들어왔을 때 아이템 1회만 구매됨")
        void successPurchaseItemWithBothThread() throws Exception {
            // ========= Given =========
            int threadCount = 2; // 실행할 스레드 개수
            // 1. 카운트다운 래치(대기표) 생성 - 모든 Thread의 작업이 끝날 때까지 기다리기 위한 장치 역할을 함
            CountDownLatch countDownLatch = new CountDownLatch(threadCount);
            PurchaseItemReqDto reqDto = new PurchaseItemReqDto(affordableItemId);

            // 2. 실행할 스레드만큼의 크기로 Thread Pool 생성
            try(ExecutorService executorService = Executors.newFixedThreadPool(threadCount)){
                // ========= When =========
                // 1. 생성한 Thread Pool에 속한 각 Thread에게 작업 부여
                for(int i = 0; i < threadCount; i++){
                    // 2. 각 Thread에게 작업을 부여한다.
                    // executorService.submit(): 스레드 풀에 작업을 제출할 때 사용하는 함수. 작업을 Queue에 넣고 바로 Future 객체를 반환한다.
                    executorService.submit(() -> {
                        try {
                            // 동시에 같은 아이템 구매 시도
                            shopService.purchaseItem(userId, reqDto);
                        } catch (Exception e) {
                            // 동시성 테스트에서는 늦게 도착한 스레드가 던지는 '이미 구매한 아이템' 에러를 무시하거나 로그로 확인합니다.
                            System.out.println("Expected Exception from delayed thread: " + e.getMessage());
                        } finally {
                            countDownLatch.countDown();
                        }
                    });
                }
                // 각 Thread의 작업이 끝날 때까지 대기
                countDownLatch.await();
            }

            // ========= Then =========
            // 1. DB에서 최신 유저 정보를 가져와 잉크 차감이 1번만(10 - 5 = 5) 발생했는지 검증
            User updatedUser = userRepository.findById(userId).orElseThrow();
            assertThat(updatedUser.getInk()).isEqualTo(5);

            // 2. UserItem 테이블에 해당 아이템이 1개만 생성되었는지 검증
            long purchasedItemCount = userItemRepository.count();
            assertThat(purchasedItemCount).isEqualTo(1);
        }

        @Test
        @DisplayName("[실패] 잉크가 부족한 경우 아이템 구매 실패")
        void failPurchaseItemNotEnoughInk() {
            // ========= Given =========
            PurchaseItemReqDto reqDto = new PurchaseItemReqDto(expensiveItemId);

            // ========= When & Then =========
            // CustomException이 발생하고, 에러 코드가 INSUFFICIENT_INK_TO_BUY_ITEM인지 검증
            assertThatThrownBy(() -> shopService.purchaseItem(userId, reqDto))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining(ShopErrorCode.INSUFFICIENT_INK_TO_BUY_ITEM.getMessage()); // 에러 메시지나 코드를 검증 (구현하신 에러 형태에 맞춰 수정)
        }

    }

}
