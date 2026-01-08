package com.example.egobook_be.global.config;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class QueryDslConfig {
    // EntityManager: JPA 영속성 컨텍스트를 관리하는 인터페이스
    private final EntityManager entityManager;

    /**
     * JPAQueryFactory: JPA 환경에서 타입 안전한 쿼리를 쉽게 작성하도록 도와주는 클래스
     * EntityManager를 기반으로 생성되며, 실제 결과를 가져오는 역할도 한다.
     * @return JPAQueryFactory
     */
    @Bean
    public JPAQueryFactory jpaQueryFactory(){
        return new JPAQueryFactory(entityManager);
    }
}
