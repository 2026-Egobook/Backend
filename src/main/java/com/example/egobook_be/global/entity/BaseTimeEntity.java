package com.example.egobook_be.global.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * @MappedSuperclass: JPA 어노테이션
 * - 해당 클래스 자체는 엔티티 테이블로 매핑되지 않으나, 대신 공통 속성을 자식 엔티티들이 상속받아 사용할 수 있다.
 */
@MappedSuperclass
/**
 * @EntityListeners(AuditingEntityListener.class): JPA 엔티티 리스너 등록
 * - 엔티티의 생명주기 이벤트(생성, 수정 등)를 감지해서 특정 동작을 수행한다.
 * - AuditingEntityListener를 등록해서, @CreatedDate, @LastModifiedDate 같은 필드를 자동으로 채워준다.
 *      => AuditingEntityListener: Spring Data JPA 제공 클래스
 *      - 엔티티가 persist(저장)되거나 update(수정)될 때 이벤트를 가로채서, 날짜/사용자 정보를 자동으로 기록함.
 *      - @EnableJpaAuditing을 활성화해야 동작한다.
 */
@EntityListeners(AuditingEntityListener.class)
@Getter
public abstract class BaseTimeEntity {
    @CreatedDate // 엔티티가 처음 저장될 때 자동으로 현재 시간이 들어감
    @Column(updatable = false) // updatable를 false로 설정하면 이후 수정 시 값이 변경되지 않는다.
    private LocalDateTime createdAt;

    @LastModifiedDate // 엔티티가 수정될 때 자동으로 현재 시간이 들어감. 매번 업데이트 시 갱신됨
    private LocalDateTime updatedAt;
}