
package com.example.egobook_be.domain.shop.service;

import com.example.egobook_be.domain.shop.dto.AdminItemReqDto;
import com.example.egobook_be.domain.shop.entity.Item;
import com.example.egobook_be.domain.shop.enums.ItemCategory;
import com.example.egobook_be.domain.shop.exception.ShopErrorCode;
import com.example.egobook_be.domain.shop.repository.ItemRepository;
import com.example.egobook_be.global.exception.CustomException;
import com.example.egobook_be.infra.s3.S3ImageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminShopServiceUnitTest {

    @InjectMocks
    private AdminShopService adminShopService;

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private S3ImageService s3ImageService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(
                adminShopService,
                "cloudfrontDomain",
                "https://test.cloudfront.net"
        );
    }

    private MockMultipartFile createTestFile() {
        return new MockMultipartFile(
                "file",
                "test.png",
                "image/png",
                "test image".getBytes()
        );
    }

    @Nested
    @DisplayName("아이템 등록 테스트")
    class CreateItemTest {

        @Test
        @DisplayName("상태값이 없으면 기본 비활성으로 등록")
        void createItemDefaultInactive() throws IOException {
            // Given
            MockMultipartFile file = createTestFile();

            AdminItemReqDto reqDto = new AdminItemReqDto(
                    ItemCategory.BACK, 100, file, null
            );

            given(s3ImageService.upload(file, "back"))
                    .willReturn(
                            "https://test.cloudfront.net/shop/back/test.png"
                    );

            given(itemRepository.save(any(Item.class)))
                    .willAnswer(invocation ->
                            invocation.getArgument(0));

            // When
            adminShopService.createItem(reqDto);

            // Then
            ArgumentCaptor<Item> captor =
                    ArgumentCaptor.forClass(Item.class);

            verify(itemRepository).save(captor.capture());

            assertThat(captor.getValue().getStatus())
                    .isEqualTo("INACTIVE");
        }

        @Test
        @DisplayName("명시적으로 활성 상태를 선택하면 활성으로 등록")
        void createActiveItem() throws IOException {
            // Given
            MockMultipartFile file = createTestFile();

            AdminItemReqDto reqDto = new AdminItemReqDto(
                    ItemCategory.BACK, 100, file, "ACTIVE"
            );

            given(s3ImageService.upload(file, "back"))
                    .willReturn(
                            "https://test.cloudfront.net/shop/back/test.png"
                    );

            given(itemRepository.save(any(Item.class)))
                    .willAnswer(invocation ->
                            invocation.getArgument(0));

            // When
            adminShopService.createItem(reqDto);

            // Then
            ArgumentCaptor<Item> captor =
                    ArgumentCaptor.forClass(Item.class);

            verify(itemRepository).save(captor.capture());

            assertThat(captor.getValue().getStatus())
                    .isEqualTo("ACTIVE");
        }

        @Test
        @DisplayName("잘못된 상태값이면 등록 실패")
        void createItemInvalidStatus() {
            // Given
            AdminItemReqDto reqDto = new AdminItemReqDto(
                    ItemCategory.BACK,
                    100,
                    createTestFile(),
                    "INVALID"
            );

            // When & Then
            assertThatThrownBy(() ->
                    adminShopService.createItem(reqDto))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> assertThat(
                            ((CustomException) ex).getErrorCode()
                    ).isEqualTo(
                            ShopErrorCode.INVALID_ITEM_STATUS
                    ));

            verifyNoInteractions(s3ImageService);
            verify(itemRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("아이템 수정 테스트")
    class UpdateItemTest {

        @Test
        @DisplayName("파일 없이 가격 수정 시 기존 이미지 유지")
        void updateItemWithoutFile() throws IOException {
            // Given
            Long itemId = 1L;

            Item item = Item.builder()
                    .category(ItemCategory.BACK)
                    .price(100)
                    .path("back")
                    .name("old.png")
                    .status("ACTIVE")
                    .build();

            AdminItemReqDto reqDto = new AdminItemReqDto(
                    ItemCategory.BACK,
                    200,
                    null,
                    null
            );

            given(itemRepository.findById(itemId))
                    .willReturn(Optional.of(item));

            given(itemRepository.save(any(Item.class)))
                    .willAnswer(invocation ->
                            invocation.getArgument(0));

            // When
            adminShopService.updateItem(itemId, reqDto);

            // Then
            assertThat(item.getPrice()).isEqualTo(200);
            assertThat(item.getName()).isEqualTo("old.png");
            assertThat(item.getPath()).isEqualTo("back");
            assertThat(item.getStatus()).isEqualTo("ACTIVE");

            verifyNoInteractions(s3ImageService);
        }

        @Test
        @DisplayName("수정 시 비활성 상태로 변경")
        void updateItemInactive() throws IOException {
            // Given
            Long itemId = 1L;

            Item item = Item.builder()
                    .category(ItemCategory.BACK)
                    .price(100)
                    .path("back")
                    .name("old.png")
                    .status("ACTIVE")
                    .build();

            AdminItemReqDto reqDto = new AdminItemReqDto(
                    ItemCategory.BACK,
                    100,
                    null,
                    "INACTIVE"
            );

            given(itemRepository.findById(itemId))
                    .willReturn(Optional.of(item));

            given(itemRepository.save(any(Item.class)))
                    .willAnswer(invocation ->
                            invocation.getArgument(0));

            // When
            adminShopService.updateItem(itemId, reqDto);

            // Then
            assertThat(item.getStatus())
                    .isEqualTo("INACTIVE");

            verify(itemRepository).save(item);
        }

        @Test
        @DisplayName("이미지 교체 시 S3에 저장된 파일명 사용")
        void updateItemWithNewFile() throws IOException {
            // Given
            Long itemId = 1L;
            MockMultipartFile file = createTestFile();

            Item item = Item.builder()
                    .category(ItemCategory.BACK)
                    .price(100)
                    .path("back")
                    .name("old.png")
                    .status("ACTIVE")
                    .build();

            AdminItemReqDto reqDto = new AdminItemReqDto(
                    ItemCategory.BACK,
                    100,
                    file,
                    "ACTIVE"
            );

            given(itemRepository.findById(itemId))
                    .willReturn(Optional.of(item));

            given(s3ImageService.upload(file, "back"))
                    .willReturn(
                            "https://test.cloudfront.net/shop/back/new-uuid.png"
                    );

            given(itemRepository.save(any(Item.class)))
                    .willAnswer(invocation ->
                            invocation.getArgument(0));

            // When
            adminShopService.updateItem(itemId, reqDto);

            // Then
            assertThat(item.getName())
                    .isEqualTo("new-uuid.png");

            verify(s3ImageService).upload(file, "back");
        }
    }
}
  