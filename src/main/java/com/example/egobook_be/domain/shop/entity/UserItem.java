package com.example.egobook_be.domain.shop.entity;

import com.example.egobook_be.domain.user.entity.User; // User Entity import 필요
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
// Private로 Access를 막아둠으로써, 외부 코드에서 new User(...)로 생성하는 것을 금지시킨다.
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class) // purchased_at 자동 생성을 위해 필수
@Table(name = "user_item",
        // (userId, itemId) Unique 제약조건
        uniqueConstraints = {
            @UniqueConstraint(
                    name="uk_user_item_provider", // 제약조건 이름 명시
                    columnNames = {"user_id", "item_id"}) // 묶을 컬럼들 이름 설정
        }
)
public class UserItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 1. 유저 (FK): User 테이블과 연결
    // FetchType.LAZY는 필수입니다. (성능 최적화)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 2. 아이템 (FK): Item 테이블과 연결
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    // 3. 장착 여부 (기본값 false)
    @Column(name = "is_equipped", nullable = false)
    @Builder.Default
    private Boolean isEquipped = false;

    // 4. 구매 일시 (데이터 생성 시 자동 기록)
    @CreatedDate
    @Column(name = "purchased_at", nullable = false, updatable = false)
    private LocalDateTime purchasedAt;


    // --- 비즈니스 로직 (상태 변경 메서드) ---

    // 아이템 장착
    public void equip() {
        this.isEquipped = true;
    }

    // 아이템 해제
    public void unequip() {
        this.isEquipped = false;
    }

    public static UserItem create(User user, Item item) {
        return UserItem.builder()
                .user(user)
                .item(item)
                .isEquipped(false)
                .build();
    }


}