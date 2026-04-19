package com.example.egobook_be.domain.shop.service;

import com.example.egobook_be.domain.shop.dto.ItemInfoResDto;
import com.example.egobook_be.domain.shop.dto.PurchaseItemReqDto;
import com.example.egobook_be.domain.shop.entity.Item;
import com.example.egobook_be.domain.shop.entity.UserItem;
import com.example.egobook_be.domain.shop.enums.ShopErrorCode;
import com.example.egobook_be.domain.shop.mapper.ItemMapper;
import com.example.egobook_be.domain.shop.mapper.UserItemMapper;
import com.example.egobook_be.domain.shop.repository.ItemRepository;
import com.example.egobook_be.domain.shop.repository.UserItemRepository;
import com.example.egobook_be.domain.shop.sevice.ShopService;
import com.example.egobook_be.domain.user.entity.User;
import com.example.egobook_be.domain.user.exception.UserErrorCode;
import com.example.egobook_be.domain.user.repository.UserRepository;
import com.example.egobook_be.global.exception.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class) // Mockito 사용을 위해 필수
public class ShopServiceUnitTest {

    @InjectMocks private ShopService shopService;
    @Mock private ItemRepository itemRepository;
    @Mock private UserItemRepository userItemRepository;
    @Mock private UserRepository userRepository;
    @Mock private ItemMapper itemMapper;
    @Mock private UserItemMapper userItemMapper;

    @BeforeEach
    void setUp() {
        // @Value 필드에 테스트용 가짜 도메인 주입
        ReflectionTestUtils.setField(shopService, "cloudfrontDomain", "https://test.cloudfront.net");
    }

    @Nested
    @DisplayName("purchaseItem() 메서드 테스트 코드")
    class PurchaseItemTest {

        @Test
        @DisplayName("[성공] 아이템 구매 성공")
        void successPurchaseItem() {
            // ========= Given =========
            Long userId = 1L;
            Long itemId = 100L;
            PurchaseItemReqDto reqDto = new PurchaseItemReqDto(itemId); // 레코드로 가정

            Item mockItem = mock(Item.class);
            User mockUser = mock(User.class);
            UserItem mockUserItem = mock(UserItem.class);
            ItemInfoResDto expectedResDto = mock(ItemInfoResDto.class);

            given(mockItem.getPrice()).willReturn(50);
            given(mockUser.getInk()).willReturn(100); // 아이템 가격(50)보다 많은 잉크 보유

            given(itemRepository.findById(itemId)).willReturn(Optional.of(mockItem));
            given(userItemRepository.existsByUserIdAndItemId(userId, itemId)).willReturn(false);
            given(userRepository.findByIdWithLock(userId)).willReturn(Optional.of(mockUser));
            given(userItemRepository.save(any(UserItem.class))).willReturn(mockUserItem);
            given(userItemMapper.toItemInfoResDto(any(), any(), anyString())).willReturn(expectedResDto);

            // ========= When =========
            ItemInfoResDto result = shopService.purchaseItem(userId, reqDto);

            // ========= Then =========
            assertThat(expectedResDto).isEqualTo(result);
            verify(mockUser, times(1)).useInk(50); // 잉크 차감 메서드가 호출되었는지 검증
            verify(userItemRepository, times(1)).save(any(UserItem.class)); // 저장 메서드 호출 검증
        }

        @Test
        @DisplayName("[실패 1] 존재하지 않는 아이템 구매 시도")
        void failPurchaseNotFoundItem() {
            // ========= Given =========
            Long userId = 1L;
            Long itemId = 999L;
            PurchaseItemReqDto reqDto = new PurchaseItemReqDto(itemId);

            given(itemRepository.findById(itemId)).willReturn(Optional.empty());

            // ========= When & Then =========
            CustomException exception = assertThrows(CustomException.class,
                    () -> shopService.purchaseItem(userId, reqDto));

            assertThat(exception.getErrorCode()).isEqualTo(ShopErrorCode.ITEM_NOT_FOUND); // 에러 코드 검증

            // 이후 로직들이 실행되지 않았는지 검증
            verify(userRepository, never()).findByIdWithLock(any());
            verify(userItemRepository, never()).save(any());
        }

        @Test
        @DisplayName("[실패 2] 이미 구매한 아이템 재구매 시도")
        void failAlreadyPurchasedItem() {
            // ========= Given =========
            Long userId = 1L;
            Long itemId = 100L;
            PurchaseItemReqDto reqDto = new PurchaseItemReqDto(itemId);
            Item mockItem = mock(Item.class);

            given(itemRepository.findById(itemId)).willReturn(Optional.of(mockItem));
            given(userItemRepository.existsByUserIdAndItemId(userId, itemId)).willReturn(true); // 이미 구매함

            // ========= When & Then =========
            CustomException exception = assertThrows(CustomException.class,
                    () -> shopService.purchaseItem(userId, reqDto));

            assertThat(exception.getErrorCode()).isEqualTo(ShopErrorCode.ALREADY_PURCHASED_ITEM);
            verify(userRepository, never()).findByIdWithLock(any());
        }

        @Test
        @DisplayName("[실패 3] 유저를 찾을 수 없음")
        void failUserNotFound() {
            // ========= Given =========
            Long userId = 1L;
            Long itemId = 100L;
            PurchaseItemReqDto reqDto = new PurchaseItemReqDto(itemId);
            Item mockItem = mock(Item.class);

            given(itemRepository.findById(itemId)).willReturn(Optional.of(mockItem));
            given(userItemRepository.existsByUserIdAndItemId(userId, itemId)).willReturn(false);
            given(userRepository.findByIdWithLock(userId)).willReturn(Optional.empty()); // 유저 없음

            // ========= When & Then =========
            CustomException exception = assertThrows(CustomException.class,
                    () -> shopService.purchaseItem(userId, reqDto));

            assertThat(exception.getErrorCode()).isEqualTo(UserErrorCode.USER_NOT_FOUND);
        }

        @Test
        @DisplayName("[실패 4] 잉크 부족으로 아이템 구매 실패")
        void failNotEnoughInkToPurchaseItem() {
            // ========= Given =========
            Long userId = 1L;
            Long itemId = 100L;
            PurchaseItemReqDto reqDto = new PurchaseItemReqDto(itemId);

            Item mockItem = mock(Item.class);
            User mockUser = mock(User.class);

            given(mockItem.getPrice()).willReturn(150); // 아이템 가격 150
            given(mockUser.getInk()).willReturn(100);  // 유저 보유 잉크 100 (부족)

            given(itemRepository.findById(itemId)).willReturn(Optional.of(mockItem));
            given(userItemRepository.existsByUserIdAndItemId(userId, itemId)).willReturn(false);
            given(userRepository.findByIdWithLock(userId)).willReturn(Optional.of(mockUser));

            // ========= When & Then =========
            CustomException exception = assertThrows(CustomException.class,
                    () -> shopService.purchaseItem(userId, reqDto));

            assertThat(exception.getErrorCode()).isEqualTo(ShopErrorCode.INSUFFICIENT_INK_TO_BUY_ITEM);

            // 재화 차감 및 저장이 발생하지 않았는지 검증
            verify(mockUser, never()).useInk(anyInt());
            verify(userItemRepository, never()).save(any());
        }
    }
}