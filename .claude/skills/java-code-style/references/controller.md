# Controller 계층 규칙

## XXXControllerDocs.java

- Swagger 문서 전용 인터페이스
- `@Tag`, `@RequestMapping`, `@Operation`, `@ApiResponses`, `@SecurityRequirement` 등 **Swagger 애노테이션 전부 여기에 정의**
- `@Parameter`로 각 파라미터 설명 작성
- 실제 비즈니스 로직 없음

**기본 예시 (단순 조회):**

```java
@Tag(name = "User", description = "사용자 관련 API")
@RequestMapping("/api/v1/users")
public interface UserControllerDocs {

    @Operation(summary = "사용자 조회", description = "ID로 사용자를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = UserResDto.class))),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없습니다.",
                    content = @Content)
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/{userId}")
    ResponseEntity<GlobalResponse<UserResDto>> getUser(@PathVariable Long userId);
}
```

**실사용 예시 (검색 + 필터 + 페이징 + 상세 description):**

```java
@Tag(name = "Admin User", description = "관리자 - 회원 관리 API")
@RequestMapping("/api/v1/admin/users")
public interface AdminUserControllerDocs {

    @Operation(summary = "회원 리스트 검색", description = """
            키워드 & 필터 검색을 통해 회원들을 리스트로 검색하는 API입니다.
            
            [**Query Parameter**]
            - page: 페이지 번호 (1 ~ n)
            - size: 페이지 크기
            - keyword: 검색창에 작성한 검색 키워드
                (검색할 수 있는 요소= `Account Code` & `Email` & `Nickname`)
            - status: 검색할 사용자의 상태 필터
                (필터링할 수 있는 요소)
                    1. `ACTIVE` (활동 상태)
                    2. `DORMANT` (휴면 상태)
                    3. `WITHDRAW_PENDING` (탈퇴 대기 상태)
                    4. `SUSPENDED` (정지 상태)
            """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "회원 리스트 검색 성공",
                    content = @Content(schema = @Schema(implementation = SliceResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 상태 필터 값을 보냈습니다.",
                    content = @Content),
            @ApiResponse(responseCode = "401", description = "로그인이 필요합니다.",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "관리자 권한이 필요합니다.",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "해당 키워드 및 필터에 맞는 사용자 정보들을 찾지 못했습니다.",
                    content = @Content)
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("")
    ResponseEntity<GlobalResponse<SliceResponse<SearchUserResDto>>> searchUserList(
            @Parameter(description = "검색 키워드 (AccountCode | Email | Nickname)", required = true)
            @RequestParam("keyword") String keyword,

            @Parameter(description = "사용자 상태 필터 (ACTIVE | DORMANT | WITHDRAW_PENDING | SUSPENDED)")
            @RequestParam(value = "status", required = false) UserStatus status,

            @Parameter(description = "Page 번호 (1 ~ N)", required = true)
            @RequestParam(value = "page", defaultValue = "1") Integer page,

            @Parameter(description = "Page 크기", required = true)
            @RequestParam(value = "size", defaultValue = "5") Integer size
    );
}
```

**Swagger 작성 규칙 요약:**

| 항목 | 규칙 |
|---|---|
| `@Operation` | `summary`는 한 줄 요약, `description`은 여러 줄 상세 설명 (파라미터 설명 포함) |
| `@ApiResponses` | 가능한 모든 응답 코드 명시. 응답 body 없는 경우 `content = @Content` |
| `@Parameter` | 모든 `@RequestParam`, `@PathVariable`에 작성. `required` 여부 명시 |
| `@SecurityRequirement` | 인증이 필요한 API에 `name = "bearerAuth"` 추가 |
| `content = @Content(schema = @Schema(implementation = XXX.class))` | 200 응답에 실제 반환 DTO 클래스 명시 |

---

## XXXController.java

- `XXXControllerDocs`를 `implements`
- **`@Override`만 사용하여 구현**, Swagger 애노테이션 중복 작성 금지
- 로직 없이 Service 호출 및 응답 반환만 수행
- 클래스 레벨: `@RestController`, `@RequiredArgsConstructor`

```java
@RestController
@RequiredArgsConstructor
public class UserController implements UserControllerDocs {

    private final UserService userService;

    @Override
    public ResponseEntity<GlobalResponse<UserResDto>> getUser(@PathVariable Long userId) {
        return ResponseEntity.ok(GlobalResponse.success(userService.getUser(userId)));
    }

    @Override
    public ResponseEntity<GlobalResponse<SliceResponse<SearchUserResDto>>> searchUserList(
            String keyword, UserStatus status, Integer page, Integer size) {
        return ResponseEntity.ok(GlobalResponse.success(
                userService.searchUserList(keyword, status, page, size)));
    }
}
```
