package com.example.egobook_be.domain.user.entity;

import com.example.egobook_be.domain.user.enums.WithdrawReasonType;
import com.example.egobook_be.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "withdraw_reason")
public class WithdrawReason extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // User 삭제 시 WithdrawReason 엔티티의 인스턴스도 삭제되는 것을 방지하기 위해 연관관계가 아닌 userId값만 참조
    // User 한명당 하나의 탈퇴 이유를 작성해야하므로 unique 속성 설정
    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason_type", nullable = false)
    private WithdrawReasonType withdrawReasonType;

    @Column(length = 500)
    private String text;

    public static WithdrawReason create(Long userId, WithdrawReasonType withdrawReasonType, String text) {
        return WithdrawReason.builder().userId(userId).withdrawReasonType(withdrawReasonType).build();
    }
}
