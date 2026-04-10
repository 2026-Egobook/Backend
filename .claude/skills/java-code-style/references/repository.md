# Repository 계층 규칙

## XXXRepository.java

- `JpaRepository<ClassName, Long>` 형식으로 extends
- 모든 커스텀 메서드에 JavaDoc 주석 필수

```java
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * 신규 사용자 가입 시, Nickname이 이미 존재하는지 확인하기 위한 함수
     * @param nickname : 중복 여부를 확인할 닉네임
     * @return : 닉네임 존재 여부 (true: 이미 존재, false: 사용 가능)
     */
    boolean existsByNickname(String nickname);

    /**
     * 이메일로 사용자 단건 조회
     * @param email : 조회할 이메일
     * @return : Optional<User>
     */
    Optional<User> findByEmail(String email);
}
```

---

## QueryDSL 적용 시

파일 3개 세트로 구성:

### 1. XXXRepository.java — QueryDSL 인터페이스 상속 추가

```java
public interface UserRepository extends JpaRepository<User, Long>, UserRepositoryCustom {
    // JPA 메서드 + QueryDSL 메서드 모두 사용 가능
}
```

### 2. XXXRepositoryCustom.java — QueryDSL 메서드 인터페이스 정의

```java
public interface UserRepositoryCustom {

    /**
     * 키워드 & 상태 필터로 사용자 목록 Slice 조회
     * @param keyword  : 검색 키워드 (AccountCode | Email | Nickname 대상)
     * @param status   : 사용자 상태 필터 (null이면 전체 조회)
     * @param pageable : 페이징 정보
     * @return : Slice<User>
     */
    Slice<User> searchByKeywordAndStatus(String keyword, UserStatus status, Pageable pageable);
}
```

### 3. XXXRepositoryCustomImpl.java — QueryDSL 구현체

```java
@RequiredArgsConstructor
public class UserRepositoryCustomImpl implements UserRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Slice<User> searchByKeywordAndStatus(
            String keyword, UserStatus status, Pageable pageable) {

        List<User> content = queryFactory
                .selectFrom(user)
                .where(
                        keywordContains(keyword),
                        statusEq(status)
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize() + 1L) // hasNext 판단을 위해 +1
                .fetch();

        boolean hasNext = content.size() > pageable.getPageSize();
        if (hasNext) content.remove(content.size() - 1);

        return new SliceImpl<>(content, pageable, hasNext);
    }

    // BooleanExpression으로 조건 분리 (null이면 조건 무시됨)
    private BooleanExpression keywordContains(String keyword) {
        if (keyword == null || keyword.isBlank()) return null;
        return user.accountCode.containsIgnoreCase(keyword)
                .or(user.email.containsIgnoreCase(keyword))
                .or(user.nickname.containsIgnoreCase(keyword));
    }

    private BooleanExpression statusEq(UserStatus status) {
        return status != null ? user.status.eq(status) : null;
    }
}
```
