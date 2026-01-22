package com.example.egobook_be.domain.shop.sevice;

import com.example.egobook_be.domain.shop.dto.ItemInfoResDto;
import com.example.egobook_be.domain.shop.dto.UserItemStatusDto;
import com.example.egobook_be.domain.shop.entity.Item;
import com.example.egobook_be.domain.shop.enums.ItemCategory;
import com.example.egobook_be.domain.shop.mapper.ItemMapper;
import com.example.egobook_be.domain.shop.repository.ItemRepository;
import com.example.egobook_be.domain.shop.repository.UserItemRepository;
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
    private final ItemMapper itemMapper;

    // 프론트가 접속할 cloudfront의 도메인 주소
    @Value("${spring.cloud.aws.cloudfront.domain}")
    private String cloudfrontDomain;
    
    /**
     * 특정 카테고리의 아이템을 무한 스크롤이 가능하도록 Slice로 가져와서 반환하는 api
     * @param userId 요청을 한 user의 PK
     * @param itemCategory 가져올 아이템의 카테고리
     * @param slice 반환할 Slice 번호
     * @param size 한개의 Slice에 들어있는 요소의 개수
     * @return
     */
    @Transactional(readOnly = true)
    public SliceResponse<ItemInfoResDto> getItemSlice(Long userId, ItemCategory itemCategory, Integer slice, Integer size){
        /*
         * 1. Slicing을 위한 Pageable 객체 생성 (아이템 가격 기준으로 오름차순 정렬)
         * - 프론트로부터는 Slice값이 1 ~ N으로 오기 때문에, 해당 값을 -1
         * [ 예외 ]
         * (1) 입력된 Slice값이 0보다 작은 경우
         */
        if(slice < 0) throw new CustomException(GlobalErrorCode.INVALID_SLICE_VALUE);
        int pageIndex = (slice != null && slice >= 1) ? slice - 1 : 0;
        Pageable pageable = PageRequest.of(pageIndex, size, Sort.by(Sort.Direction.ASC, "price"));

        // 2. 해당 카테고리에 해당하는 Item들 slice로 조회
        Slice<Item> sliceEntity = itemRepository.findByCategory(itemCategory, pageable);

        /*
         * 3. 조회한 아이템들 중, 해당 User가 구매한 item(UserItem)들의 Set을 생성한다.
         * (1) 기존 Slice로 조회한 item 목록에서 item ID만 추출
         *  - getContent(): Slice에서 List로 내용물을 꺼내오는 함수
         * (2) 추출한 item Id들만큼 UserItem 테이블에서 데이터들 가져오기 (UserItem PK들만 가져온다)
         * (3) 가져온 Set을 Map<ItemId, IsEquipped> 형식으로 변환
         */
        List<Long> itemIds = sliceEntity.getContent().stream().map(Item::getId).toList();
        Set<UserItemStatusDto> userItemSet = itemIds.isEmpty() ? Collections.emptySet() : userItemRepository.findUserItemIdSetByItem(userId, itemIds);
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
            return itemMapper.toItemInfoResDto(item, cloudfrontDomain, isPurchased, userItemMap.getOrDefault(item.getId(), false));
        });
    }


}
