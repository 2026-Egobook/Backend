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
@Table(name = "item")
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

    /*
     * 이 아이템을 보유한 유저 매핑 리스트 (양방향 매핑)
     * 아이템 자체가 삭제되면, 해당 아이템의 보유 기록(UserItem)도 삭제됨
     */
    @OneToMany(mappedBy = "item", cascade = CascadeType.ALL, orphanRemoval = true)
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
}