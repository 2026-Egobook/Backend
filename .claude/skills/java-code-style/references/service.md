# Service 계층 규칙

## XXXService.java

- 클래스 레벨: `@Service`, `@RequiredArgsConstructor`, `@Slf4j`
- **모든 함수**에 아래 3가지 규칙 필수 적용:

### 규칙 1: JavaDoc 주석
함수 목적, 동작 상세 설명, `@param`, `@return` 기술

### 규칙 2: 함수 시작 로깅
```java
log.info("[XXXService] functionName() - START | param: {}", param);
```

### 규칙 3: 함수 종료 로깅
```java
log.info("[XXXService] functionName() - END | result: {}", result);
```

---

## 작성 예시

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    /**
     * User Entity 생성 공통 로직
     * - AccountCode, Nickname을 자동으로 생성해준다.
     * - email은 선택적으로 넣을 수 있다.
     * - User 생성 후, userRepository에 save()까지 수행한 결과물을 반환한다.
     * @param email : Guest-null, Google-Token에 있는 Google Email 설정
     * @return : 저장된 User Entity
     */
    public UserResDto createUser(String email) {
        log.info("[UserService] createUser() - START | email: {}", email);

        User user = User.builder()
                .email(email)
                .nickname(generateNickname())
                .accountCode(generateAccountCode())
                .build();
        User savedUser = userRepository.save(user);
        UserResDto result = userMapper.toUserResDto(savedUser);

        log.info("[UserService] createUser() - END | userId: {}", savedUser.getId());
        return result;
    }

    /**
     * userId로 사용자 단건 조회
     * - 존재하지 않는 userId인 경우 UserErrorCode.USER_NOT_FOUND 예외 발생
     * @param userId : 조회할 사용자 ID
     * @return : 조회된 UserResDto
     */
    public UserResDto getUser(Long userId) {
        log.info("[UserService] getUser() - START | userId: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));
        UserResDto result = userMapper.toUserResDto(user);

        log.info("[UserService] getUser() - END | userId: {}", userId);
        return result;
    }
}
```

---

## 무한스크롤 (SliceResponse) 반환 시

```java
/**
 * 키워드 & 상태 필터로 사용자 목록 Slice 조회
 * @param keyword : 검색 키워드 (AccountCode | Email | Nickname)
 * @param status  : 사용자 상태 필터 (null 허용 시 전체 조회)
 * @param page    : 페이지 번호 (1 ~ N, 프론트 기준)
 * @param size    : 페이지 크기
 * @return : SliceResponse<SearchUserResDto>
 */
public SliceResponse<SearchUserResDto> searchUserList(
        String keyword, UserStatus status, Integer page, Integer size) {
    log.info("[UserService] searchUserList() - START | keyword: {}, status: {}", keyword, status);

    Pageable pageable = PageRequest.of(page - 1, size); // 프론트 page는 1부터 시작
    Slice<User> slice = userRepository.searchByKeywordAndStatus(keyword, status, pageable);
    SliceResponse<SearchUserResDto> result = SliceResponse.of(slice, userMapper::toSearchUserResDto);

    log.info("[UserService] searchUserList() - END | resultSize: {}", result.size());
    return result;
}
```
