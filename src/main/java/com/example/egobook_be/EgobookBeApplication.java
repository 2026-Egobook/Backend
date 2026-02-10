package com.example.egobook_be;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
/**
 * @EnableJpaAuditing: Spring Data JPA에서 감사(Auditing) 기능을 활성화 하는 어노테이션
 * - 엔티티가 언제 생성되었는지, 언제 수정했는지, 누가 생성/수정했는 지 등의 메타데이터를 자동으로 기록해주는 기능을 키는 역할
 */
@EnableJpaAuditing
@EnableScheduling // @Scheduled를 사용하기 위한 어노테이션
public class EgobookBeApplication {

    @PostConstruct
    public void started() {
        // 애플리케이션의 기본 타임존을 서울(KST)로 강제 설정
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
    }

    public static void main(String[] args) {
        SpringApplication.run(EgobookBeApplication.class, args);
    }
}
