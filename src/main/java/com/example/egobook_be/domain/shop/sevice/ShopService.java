package com.example.egobook_be.domain.shop.sevice;

import com.example.egobook_be.domain.shop.dto.*;
import com.example.egobook_be.domain.shop.entity.Item;
import com.example.egobook_be.domain.shop.entity.UserItem;
import com.example.egobook_be.domain.shop.enums.ItemCategory;
import com.example.egobook_be.domain.shop.enums.ShopErrorCode;
import com.example.egobook_be.domain.shop.mapper.ItemMapper;
import com.example.egobook_be.domain.shop.mapper.UserItemMapper;
import com.example.egobook_be.domain.shop.repository.ItemRepository;
import com.example.egobook_be.domain.shop.repository.UserItemRepository;
import com.example.egobook_be.domain.user.entity.User;
import com.example.egobook_be.domain.user.enums.UserErrorCode;
import com.example.egobook_be.domain.user.repository.UserRepository;
import com.example.egobook_be.global.exception.CustomException;
import com.example.egobook_be.global.exception.GlobalErrorCode;
import com.example.egobook_be.global.response.SliceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ShopService {
    private final ItemRepository itemRepository;
    private final UserItemRepository userItemRepository;
    private final UserRepository userRepository;
    private final ItemMapper itemMapper;
    private final UserItemMapper userItemMapper;

    // 프론트가 접속할 cloudfront의 도메인 주소
    @Value("${spring.cloud.aws.cloudfront.domain}")
    private String cloudfrontDomain;
    
    /**
     * 특정 카테고리의 아이템을 무한 스크롤이 가능하도록 Slice로 가져와서 반환하는 api
     * [ 아이템의 s3 url은 shop용 url을 반환한다 ]
     * @param userId 요청을 한 user의 PK
     * @param category 가져올 아이템의 카테고리
     * @param page 반환할 Slice 번호
     * @param size 한개의 Slice에 들어있는 요소의 개수
     * @return
     */
    @Transactional(readOnly = true)
    public SliceResponse<ShopItemInfoResDto> getItemSlice(Long userId, ItemCategory category, Integer page, Integer size){
        /*
         * 1. Slicing을 위한 Pageable 객체 생성 (아이템 가격 기준으로 오름차순 정렬)
         * - 프론트로부터는 Slice값이 1 ~ N으로 오기 때문에, 해당 값을 -1
         * [ 예외 ]
         * (1) 입력된 Slice값이 0보다 작은 경우나 null인 경우
         * (2) Size값이 너무 큰 경우, 최대 크기 100으로 제한 두기
         */
        if(page == null || page < 0) throw new CustomException(GlobalErrorCode.INVALID_SLICE_VALUE);
        int validSize = (size == null || size < 1) ? 6 : Math.min(size, 100);
        int pageIndex = (page >= 1) ? page - 1 : 0;
        Pageable pageable = PageRequest.of(pageIndex, validSize, Sort.by(Sort.Direction.ASC, "price"));

        // 2. 해당 카테고리에 해당하는 Item들 slice로 조회
        Slice<Item> sliceEntity = itemRepository.findByCategory(category, pageable);

        /*
         * 3. 조회한 아이템들 중, 해당 User가 구매한 item(UserItem)들의 Set을 생성한다.
         * (1) 기존 Slice로 조회한 item 목록에서 item ID만 추출
         *  - getContent(): Slice에서 List로 내용물을 꺼내오는 함수
         * (2) 추출한 item Id들만큼 UserItem 테이블에서 데이터들 가져오기 (UserItem PK들만 가져온다)
         * (3) 가져온 Set을 Map<ItemId, IsEquipped> 형식으로 변환
         */
        List<Long> itemIds = sliceEntity.getContent().stream().map(Item::getId).toList();
        Set<UserItemStatusDto> userItemSet = itemIds.isEmpty() ? Collections.emptySet() : userItemRepository.findUserItemStatusSetByItem(userId, itemIds);
        Map<Long, Boolean> userItemMap = userItemSet.stream().collect(Collectors.toMap(
                UserItemStatusDto::itemId, UserItemStatusDto::isEquipped,
                (oldValue, newValue) -> newValue
        ));

        /*
         * 4. Slice<Item> -> SliceResponse<ItemInfoResDto> Mapping
         * - 위에서 찾은 sliceEntity, userItemIds를 이용해서 Entity -> Dto로 변환
         */
        return SliceResponse.of(sliceEntity, item -> {
            Boolean isPurchased = userItemMap.containsKey(item.getId());
            return itemMapper.toShopItemInfoResDto(item, getShopCloudFrontDomain(), getMyCloudFrontDomain(), isPurchased, userItemMap.getOrDefault(item.getId(), false));
        });
    }

    /**
     * 특정 아이템을 구매(UserItem 테이블에 추가)하고, 해당 아이템의 정보를 반환해주는 함수
     * [ 상점용 아이템 이미지 url을 반환한다 ]
     * @param userId User PK
     * @param reqDto PurchaseItemReqDto
     * @return ItemInfoResDto
     */
    @Transactional
    public ItemInfoResDto purchaseItem(Long userId, PurchaseItemReqDto reqDto){
        /*
         * 1. 사용자가 보낸 Item을 검증한다.
         * - 1) item이 실제로 존재하는 Item인가?
         * - 2) 해당 사용자가 이미 구매한 Item인가?
         */
        Long itemId = reqDto.itemId();
        User user = userRepository.findByIdWithLock(userId).orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));
        Item item = itemRepository.findById(itemId).orElseThrow(() -> new CustomException(ShopErrorCode.ITEM_NOT_FOUND));
        if (userItemRepository.existsByUserIdAndItemId(userId, itemId)){
            throw new CustomException(ShopErrorCode.ALREADY_PURCHASED_ITEM);
        }

        // 2. 해당 사용자가 아이템을 살 수 있는지 확인한다.
        if(user.getInk() >= item.getPrice()){
            // 3. 해당 아이템을 구매한다. (UserItem Table에 새로운 객체를 추가한다.)
            UserItem userItem = UserItem.builder()
                    .user(user)
                    .item(item)
                    .isEquipped(false)
                    .build();
            userItem = userItemRepository.save(userItem);
            user.useInk(item.getPrice());

            // 4. 아이템 구매 후, 해당 아이템에 대한 정보를 반환한다.
            return userItemMapper.toItemInfoResDto(userItem, item, getShopCloudFrontDomain());
        }
        throw new CustomException(ShopErrorCode.INSUFFICIENT_INK_TO_BUY_ITEM);
    }

    /**
     * 아이템 착용/해제 상태를 변경하는 함수 (멱등성 보장)
     * [ 사용자용 아이템 이미지 url을 반환한다 ]
     * @param userId User PK
     * @param reqDto EquipItemReqDto (itemId, isEquipped)
     * @return ItemInfoResDto
     */
    @Transactional
    public ItemInfoResDto equipItem(Long userId, EquipItemReqDto reqDto) {
        Long itemId = reqDto.itemId();
        boolean targetStatus = reqDto.isEquipped();

        /*
         * 1. 대상 아이템 조회 (보유 여부 검증)
         * - UserItem이 없으면 구매하지 않은 아이템이므로 예외 발생 (404)
         * - N+1 방지를 위해 Fetch Join 된 메서드 사용
         */
        UserItem targetUserItem = userItemRepository.findByUserIdAndItemId(userId, itemId)
                .orElseThrow(() -> new CustomException(ShopErrorCode.ITEM_NOT_PURCHASED));

        // 2. 멱등성 체크: 이미 원하는 상태라면 DB 변경 없이 바로 반환 (불필요한 쿼리 방지)
        if (targetUserItem.getIsEquipped() == targetStatus) {
            return userItemMapper.toItemInfoResDto(targetUserItem, targetUserItem.getItem(), cloudfrontDomain);
        }

        /*
         * 3. 상태 변경 로직
         * Case A: 착용 요청 (true) -> 같은 카테고리의 기존 아이템 해제 후 착용
         * Case B: 해제 요청 (false) -> 그냥 해제
         */
        if (targetStatus) {
            // [Case A] 착용 로직
            ItemCategory category = targetUserItem.getItem().getCategory();

            // 3-1. 해당 카테고리에 이미 착용 중인 다른 아이템들을 모두 찾아서 해제
            List<UserItem> equippedItems = userItemRepository.findEquippedItemsByCategory(userId, category);
            for (UserItem equippedItem : equippedItems) {
                // 방어 로직: 혹시라도 자기 자신이 리스트에 포함되어 있다면 skip (이미 위에서 체크했지만 안전하게)
                if (!equippedItem.getId().equals(targetUserItem.getId())) {
                    equippedItem.unequip(); // isEquipped = false
                }
            }
            // 3-2. 대상 아이템 착용
            targetUserItem.equip(); // isEquipped = true

        } else {
            // [Case B] 해제 로직
            targetUserItem.unequip();
        }

        // 4. 변경된 정보 반환
        return userItemMapper.toItemInfoResDto(targetUserItem, targetUserItem.getItem(), getMyCloudFrontDomain());
    }

    /**
     * 사용자가 장착하고 있는 아이템들의 정보 리스트를 반환하는 함수
     * [ 사용자용 아이템 이미지 url을 반환한다 ]
     */
    @Transactional(readOnly = true)
    public List<ItemInfoResDto> getEquippedItems(Long userId){
        // 1. 해당 사용자가 보유하고 있는 아이템들 조회
        List<UserItem> equippedItems = userItemRepository.findEquippedItems(userId);
        // 2. 조회해온 각 아이템을 dto로 변환 (Fetch Join 썼으므로 N+1 발생 안함)
        return equippedItems.stream()
                .map(userItem -> userItemMapper.toItemInfoResDto(userItem, userItem.getItem(), getMyCloudFrontDomain()))
                .toList();
    }

    private String getShopCloudFrontDomain(){
        return cloudfrontDomain+"/shop";
    }
    private String getMyCloudFrontDomain(){
        return cloudfrontDomain+"/my";
    }
}
