package com.example.egobook_be.global.loader;

import com.example.egobook_be.domain.shop.entity.Item;
import com.example.egobook_be.domain.shop.enums.ItemCategory;
import com.example.egobook_be.domain.shop.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
@Order(value = 2) // 실행 순서
public class ItemInitializer implements ApplicationRunner {
    private final ItemRepository itemRepository;
    @Value("${spring.cloud.aws.cloudfront.domain}")
    private String cloudfrontDomain;

    // ==================================================
    // 파일 생성을 위한 변수들 선언
    // ==================================================
    String PATH_BACK = "back";
    String PATH_SKIN = "skin";
    String PATH_DECOR_ONE = "decor1";
    String PATH_DECOR_TWO = "decor2";
    String PATH_BACKGROUND = "background";


    @Override
    @Transactional
    public void run(ApplicationArguments args){
        log.info("🚀 Item 목록을 DB에 최신화합니다.");
        // 1. 등록하고 싶은 아이템 목록을 정의합니다.
        List<Item> items = getInitItemList();
        int updateCount = 0, insertCount = 0;

        /*
         * 2. 이미 DB에 존재하는 아이템들을 Map으로 가져온다.
         * - itemRepository로 모든 Item 객체를 가져와서 stream.collect()로 Map으로 변환한다.
         *  - Key: Item::getFullUrl로 만든 Full Url
         *  - Value: Item 객체
         */
        Map<String, Item> existItemMap = itemRepository.findAll().stream().collect(Collectors.toMap(
            item -> item.getFullUrl(cloudfrontDomain),
                item -> item
        ));

        // 3. DB에 존재하는 아이템들의 정보를 신규 데이터로 수정한다.
        for(Item item : items){
            /*
             * 3-1. 해당 아이템이 이미 DB에 존재하는지 확인한다.(existItemMap 활용)
             * - 해당 아이템이 이미 DB에 존재하는지 확인하면, 새로운 item의 데이터로 기존 데이터를 덮어씌운다.
             */
            String fullUrl = item.getFullUrl(cloudfrontDomain);
            if(existItemMap.containsKey(fullUrl)){
                Item existItem = existItemMap.get(fullUrl);
                existItem.updateAll(item);
                updateCount++;
            }
            // 3-2. 해당 아이템이 DB에 존재하지
            else {
                itemRepository.save(item);
                insertCount++;
            }
        }
        log.info("✅ 동기화 완료. [아이템 신규 추가: {}건, 아이템 수정: {}건]", insertCount, updateCount);
    }

    /**
     * 초기화할 아이템 리스트 정의 함수
     * @return
     */
    private List<Item> getInitItemList(){
        List<Item> items = new ArrayList<>();
        /*
         * 1. 등껍질(Back) Items 생성
         *  1-1) Default.png
         *  1-2) Yellow.png
         *  1-3) White.png
         *  1-4) Choco.png
         *  1-5) Doughnut.png
         *  1-6) Berry.png
         *  1-7) Turtle.png
         *  1-8) Mint.png
         *  1-9) CherryBlossoms.png
         *  1-10) SkyCloud.png
         */
        items.add(buildBackItem("Default.png", 0));
        items.add(buildBackItem("Yellow.png", 250));
        items.add(buildBackItem("White.png", 500));
        items.add(buildBackItem("Choco.png", 300));
        items.add(buildBackItem("Doughnut.png", 350));
        items.add(buildBackItem("Berry.png", 550));
        items.add(buildBackItem("Turtle.png", 350));
        items.add(buildBackItem("Mint.png", 400));
        items.add(buildBackItem("CherryBlossoms.png", 600));
        items.add(buildBackItem("SkyCloud.png", 450));

        /*
         * 2. 거북이 스킨(Skin) Items 생성
         *  2-1) Default.png
         *  2-2) Blue.png
         *  2-3) White.png
         *  2-4) Brown.png
         *  2-5) Black.png
         *  2-6) Pink.png
         */
        items.add(buildSkinItem("Default.png", 0));
        items.add(buildSkinItem("Blue.png", 200));
        items.add(buildSkinItem("White.png", 500));
        items.add(buildSkinItem("Brown.png", 250));
        items.add(buildSkinItem("Black.png", 300));
        items.add(buildSkinItem("Pink.png", 500));

        /*
         * 3. Decor One Items 생성
         *  3-1) Default.png
         *  3-2) Marshmallow.png
         *  3-3) Wreath.png
         *  3-4) Angel.png
         *  3-5) Apple.png
         *  3-6) CreamFruits.png
         *  3-7) RibbonGreen.png
         *  3-8) RibbonPink.png
         */
        items.add(buildDecorOneItem("Default.png", 0));
        items.add(buildDecorOneItem("Marshmallow.png", 50));
        items.add(buildDecorOneItem("Wreath.png", 400));
        items.add(buildDecorOneItem("Angel.png", 75));
        items.add(buildDecorOneItem("Apple.png", 150));
        items.add(buildDecorOneItem("CreamFruits.png", 400));
        items.add(buildDecorOneItem("RibbonGreen.png", 200));
        items.add(buildDecorOneItem("RibbonPink.png", 250));

        /*
         * 4. Decor Two Items 생성
         *  4-1) Default.png
         *  4-2) CustardCream.png
         *  4-3) Berrys.png
         *  4-4) CreamChocolate.png
         *  4-5) CreamCherry.png
         *  4-6) Ribbon.png
         *  4-7) Clover.png
         */
        items.add(buildDecorTwoItem("Default.png", 0));
        items.add(buildDecorTwoItem("CustardCream.png", 50));
        items.add(buildDecorTwoItem("Berrys.png", 400));
        items.add(buildDecorTwoItem("CreamChocolate.png", 75));
        items.add(buildDecorTwoItem("CreamCherry.png", 150));
        items.add(buildDecorTwoItem("Ribbon.png", 400));
        items.add(buildDecorTwoItem("Clover.png", 200));

        /*
         * 5. Background Items 생성
         *  5-1) Default.png
         *  5-2) Blossom.png
         *  5-3) Beach.png
         */
        items.add(buildBackgroundItem("Default.png", 0));
        items.add(buildBackgroundItem("Blossom.png", 0));
        items.add(buildBackgroundItem("Beach.png", 0));

        return items;
    }

    /**
     * 1. Back 아이템 생성 함수
     */
    private Item buildBackItem(String name, Integer price){
        return buildItem(PATH_BACK, ItemCategory.BACK, name, price);
    }
    /**
     * 2. Skin 아이템 생성 함수
     */
    private Item buildSkinItem(String name, Integer price){
        return buildItem(PATH_SKIN, ItemCategory.SKIN, name, price);
    }
    /**
     * 3. Decor1 아이템 생성 함수
     */
    private Item buildDecorOneItem(String name, Integer price){
        return buildItem(PATH_DECOR_ONE, ItemCategory.DECOR_ONE, name, price);
    }
    /**
     * 4. Decor2 아이템 생성 함수
     */
    private Item buildDecorTwoItem(String name, Integer price){
        return buildItem(PATH_DECOR_TWO, ItemCategory.DECOR_TWO, name, price);
    }
    /**
     * 5. Background 아이템 생성 함수
     */
    private Item buildBackgroundItem(String name, Integer price){
        return buildItem(PATH_BACKGROUND, ItemCategory.BACKGROUND, name, price);
    }
    /**
     * Item Entity Build하는 함수
     */
    private Item buildItem(String path, ItemCategory category, String name, Integer price){
        return Item.builder()
                .path(path)
                .category(category)
                .name(name)
                .price(price)
                .build();
    }
}
