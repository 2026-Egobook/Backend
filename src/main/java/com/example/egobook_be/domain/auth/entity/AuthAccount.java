package com.example.egobook_be.domain.auth.entity;

import com.example.egobook_be.domain.auth.enums.Provider;
import com.example.egobook_be.domain.user.entity.User;
import com.example.egobook_be.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Builder
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED) // 기본 생성자 보호 (JPA 필수)
@Table(name = "auth_account",
    // (provider, device_uid) 묶어서 Unique 제약조건 설정
    uniqueConstraints = {
        @UniqueConstraint(
                name="uk_account_device_provider", // 제약조건 이름 명시
                columnNames = {"device_uid", "provider"} // 묶을 컬럼들 이름 설정
        )
    }
)
public class AuthAccount extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "provider", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Provider provider = Provider.GUEST;

    @Column(name = "hashed_device_uid", nullable = false)
    private String hashedDeviceUid; // HmacSHA256 방식으로 해싱된 deviceUid

    @Column(name = "hashed_recover_token", length = 1000)
    private String hashedRecoverToken; // HmacSHA256 방식으로 해싱된 recoverToken

    // ========= 연관관계 매핑 ========= //

    /**
     * [Account -> User 단방향 Lazy Loading]
     * User -> AuthAccount으로의 양방향 매핑을 하지 않았음
     * User에 양방향 연결을 하면, User에서는 AuthAccount를 Lazy 로딩으로 설정하여도 구조적 한계로 인해 Eager로 동작하기 때문에 n+1 문제가 발생한다.
     * 따라서, Account만 User를 참조하도록 단방향으로 매핑시킨다.
     * => 하지만 이렇게 되었을 시, 완전 삭제 로직 동작 시 수동으로 AuthAccount 데이터들을 직접 삭제해야하는 번거로움이 발생한다.
     * => 따라서, 직접 DDL을 사용하여 User가 삭제되면 AuthAccount도 삭제되도록 설정하였다.
     *
     * [AuthAccount]
     * -- 1. 먼저 기존 제약조건의 이름을 확인합니다. (MySQL 정보 조회)
     * (출력된 CONSTRAINT_NAME을 아래 DROP 문에 넣어야 합니다. 보통 fk_auth_account_user_id 같은 형태입니다)
     * SELECT CONSTRAINT_NAME
     * FROM information_schema.KEY_COLUMN_USAGE
     * WHERE TABLE_NAME = 'auth_account'
     * AND COLUMN_NAME = 'user_id'
     * AND TABLE_SCHEMA = '데이터베이스_이름'; -- 본인의 DB 이름으로 변경
     *
     * -- 2. 기존 외래 키 삭제 (예시 이름: FK_EXISTING_NAME)
     * ALTER TABLE auth_account DROP FOREIGN KEY FK_EXISTING_NAME;
     *
     * -- 3. ON DELETE CASCADE가 포함된 새로운 외래 키 추가
     * ALTER TABLE auth_account
     * ADD CONSTRAINT fk_auth_account_user
     * FOREIGN KEY (user_id) REFERENCES user (id)
     * ON DELETE CASCADE;
     *
     * => 위 3번 작업을 수행시켜주기 위해, @OnDelete라는 어노테이션을 사용하였다.
     * @OnDelete(action = OnDeleteAction.CASCADE):
     * - Hibernate 전용 어노테이션으로, Hibernate가 DDL을 생성할 때 FOREIGN KEY 정의 뒤에 ON DELETE CASCADE 구문을 자동으로 붙여준다.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    /**
     * [양방향 매핑] AuthAccount -> RefreshTokenBackup은 Eager 로딩으로 설정하였다.
     * 그렇게 큰 데이터들이 들어가있지 않기에, 위와 같이 DDL로 직접 설정해주는 방식보다는 양방향으로 매핑하는 것을 선택하였다.
     * - CascadeType.ALL, orphanRemoval 설정도 해두었다.
      */
    @OneToOne(mappedBy = "authAccount", cascade = CascadeType.ALL, orphanRemoval = true)
    private RefreshTokenBackup refreshTokenBackup;

    // ========= Entity 비즈니스 메서드 ========= //

    /**
     * 회원가입 & refreshToken 재발급 시, AuthAccount 객체에 연결된 RefreshToken을 업데이트 해주는 함수이다.
     * @param refreshTokenBackup
     */
    public void updateRefreshTokenBackup(RefreshTokenBackup refreshTokenBackup) {
        this.refreshTokenBackup = refreshTokenBackup;
    }

    /**
     * 복구 토큰 업데이트를 위한 비즈니스 메서드
     * @param recoverToken
     */
    public void updateRecoverToken(String recoverToken) {
        this.hashedRecoverToken = recoverToken;
    }
}