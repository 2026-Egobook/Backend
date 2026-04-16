package com.example.egobook_be.domain.shop.entity;

import com.example.egobook_be.domain.shop.enums.ItemCategory;
import com.example.egobook_be.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Builder
// Private로 Access를 막아둠으로써, 외부 코드에서 new User(...)로 생성하는 것을 금지시킨다.
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA 필수: 기본 생성자 (보안상 protected 권장)
@Table(name = "item",
    // 아이템의 path/name명은 unique하도록 설정
    uniqueConstraints = {
        @UniqueConstraint(
                name = "uk_file_path_name_provider",
                columnNames = {"path","name"}
        )
    }
)
public class Item extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 1. 경로: S3 폴더 구조 (예: shop/shell) - files/는 제외하고 저장
    @Column(nullable = false, length = 255)
    private String path;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ItemCategory category;

    // 2. 파일명: 실제 파일 이름 (예: turtle-skin.png)
    @Column(nullable = false, length = 255)
    private String name;

    /*
     * 3. 가격: NULL 가능 (NULL이면 비매품 혹은 무료 등 비즈니스 로직에 따라 처리)
     * - int(원시타입) 대신 Integer(래퍼클래스)를 써야 DB의 NULL을 담을 수 있습니다.
     */
    @Column(nullable = true)
    private Integer price;

    @Column(nullable = true)
    private String status;

    /*
     * 이 아이템을 보유한 유저 매핑 리스트 (양방향 매핑)
     * 아이템 자체가 삭제되면, 해당 아이템의 보유 기록(UserItem)도 삭제됨
     * => 수정: Cascade 설정 삭제. 실수로 item을 지웠을 때, 사용자들의 아이템 구매 기록이 다같이 날아갈 수 있기 때문임.
     */
    @OneToMany(mappedBy = "item", fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private List<UserItem> userItems = new ArrayList<>();


    // --- 비즈니스 로직 (URL 조합 도우미 메서드) ---
    /**
     * 도메인을 인자로 받아 전체 CloudFront URL을 완성해주는 메서드
     * @param cloudfrontDomain 예: "https://img.egobook.site"
     * @return 예: "https://img.egobook.site/shop/shell/turtle-skin.png"
     */
    public String getFullUrl(String cloudfrontDomain) {
        return cloudfrontDomain + "/" + this.path + "/" + this.name;
    }

    /**
     * 새로운 Item 객체의 내용으로 해당 데이터를 전부 업데이트 하는 함수
     * @param item 새로운 Item 객체
     */
    public void updateAll(Item item){
        this.path = item.getPath();
        this.category = item.getCategory();
        this.name = item.getName();
        this.price = item.getPrice();
        this.status = item.getStatus();
    }

    public String getStatus() {
        return this.status == null ? "ACTIVE" : this.status;
    }

    public void activate(){
        this.status="ACTIVE";
    }
    public void deactivate(){
        this.status="INACTIVE";
    }

    public void updateAll(ItemCategory category, Integer price, String path, String name, String status) {
        this.price = price;
        this.category = category;
        this.path = path;
        this.name = name;
        this.status = status;
    }
}