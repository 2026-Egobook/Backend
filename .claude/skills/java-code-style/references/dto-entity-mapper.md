# DTO / Entity / Mapper 규칙

## DTO

- **반드시 `record` 형식**으로 작성
- **Response DTO**: `@Builder` 필수
- **Request DTO**: `@Builder` 불필요
- **네이밍**: `XXXResDto` (응답), `XXXReqDto` (요청)
- **폴더**: 별도의 req/res 하위 폴더 없이, `dto` 폴더 하나에 모두 위치

```java
// 요청 DTO
public record UserReqDto(
        String email,
        String nickname
) {}

// 응답 DTO — @Builder 필수
@Builder
public record UserResDto(
        Long userId,
        String email,
        String nickname,
        String accountCode
) {}
```

---

## Entity

- 클래스 레벨: `@Entity`, `@Getter`, `@Builder`, `@NoArgsConstructor(access = AccessLevel.PROTECTED)`, `@AllArgsConstructor`, `@Table(name = "테이블명")`
- **Enum 타입 필드는 반드시 `@Enumerated(EnumType.STRING)` 적용**
- `@Id`, `@GeneratedValue(strategy = GenerationType.IDENTITY)` 사용

```java
@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;
    private String nickname;
    private String accountCode;

    @Enumerated(EnumType.STRING)   // Enum 필드에 필수
    private UserRole role;

    @Enumerated(EnumType.STRING)   // Enum 필드에 필수
    private UserStatus status;

    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;
    private LocalDateTime deletedAt;
}
```

---

## Mapper

- Entity ↔ DTO 변환은 **반드시 Mapper 클래스를 통해서만 수행**
- DTO 내부 `from()` 정적 메서드 또는 `new XXXResDto(...)` 직접 생성 금지
- 도메인명으로 클래스 생성: `XXXMapper`
- `@Component`로 등록 → Service에서 `@RequiredArgsConstructor`로 주입
- 여러 Entity를 조합하거나 부가 정보가 필요한 경우에도 Mapper에서 처리
- **Mapper의 모든 함수에 JavaDoc 주석 필수**

```java
@Component
public class UserMapper {

    /**
     * User Entity -> UserResDto 변환
     * @param user : 변환할 User Entity
     * @return : 변환된 UserResDto
     */
    public UserResDto toUserResDto(User user) {
        return UserResDto.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .accountCode(user.getAccountCode())
                .build();
    }

    /**
     * User Entity, AuthAccount Entity -> AdminUserInfoResDto 변환
     * - 여러 Entity를 조합해야 하는 경우 Mapper에서 처리
     * @param user        : 변환할 User Entity
     * @param authAccount : 변환할 AuthAccount Entity
     * @return : 변환된 AdminUserInfoResDto
     */
    public AdminUserInfoResDto toAdminUserInfoResDto(User user, AuthAccount authAccount) {
        return AdminUserInfoResDto.builder()
                .userId(user.getId())
                .accountCode(user.getAccountCode())
                .email(user.getEmail())
                .provider(authAccount.getProvider())
                .nickname(user.getNickname())
                .createdAt(user.getCreatedAt())
                .status(user.getStatus())
                .build();
    }
}
```

---

## Enums

- 도메인 Enum은 해당 도메인의 `enums` 폴더에 위치
- 글로벌 공통 Enum은 `global/enums`에 위치
- `GlobalResponseCode`는 `global/enums`에 있으며 **변경 금지**

```java
// {도메인}/enums/UserRole.java
public enum UserRole {
    GUEST, USER, ADMIN
}

// {도메인}/enums/UserStatus.java
public enum UserStatus {
    ACTIVE, DORMANT, WITHDRAW_PENDING, SUSPENDED
}
```
