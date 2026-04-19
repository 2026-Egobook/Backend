# 테스트 코드 규칙

## 기본 규칙

- 테스트 클래스명: `XXXServiceTest`, `XXXControllerTest`
- 테스트 메서드명: `한글_설명_성공` / `한글_설명_실패_원인` 형식
- `@DisplayName`으로 한글 설명 필수
- given / when / then 주석으로 구조 구분

---

## Service 테스트

- `@ExtendWith(MockitoExtension.class)` 사용
- `@InjectMocks`로 테스트 대상 주입, `@Mock`으로 의존성 모킹
- `given(...).willReturn(...)` 패턴 사용 (BDDMockito)

```java
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @InjectMocks
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Test
    @DisplayName("userId로 사용자 조회 성공")
    void userId로_사용자_조회_성공() {
        // given
        Long userId = 1L;
        User mockUser = User.builder().id(userId).email("test@test.com").build();
        UserResDto mockDto = UserResDto.builder().userId(userId).build();

        given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
        given(userMapper.toUserResDto(mockUser)).willReturn(mockDto);

        // when
        UserResDto result = userService.getUser(userId);

        // then
        assertThat(result.userId()).isEqualTo(userId);
        verify(userRepository, times(1)).findById(userId);
        verify(userMapper, times(1)).toUserResDto(mockUser);
    }

    @Test
    @DisplayName("userId로 사용자 조회 실패 - 존재하지 않는 userId")
    void userId로_사용자_조회_실패_존재하지_않는_userId() {
        // given
        Long userId = 999L;
        given(userRepository.findById(userId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.getUser(userId))
                .isInstanceOf(CustomException.class);

        verify(userRepository, times(1)).findById(userId);
    }
}
```

---

## Controller 테스트

- `@WebMvcTest(XXXController.class)` 사용
- `@MockBean`으로 Service 모킹
- `MockMvc`로 HTTP 요청/응답 검증

```java
@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Test
    @DisplayName("사용자 단건 조회 API 성공 - 200 반환")
    void 사용자_단건_조회_API_성공() throws Exception {
        // given
        Long userId = 1L;
        UserResDto mockDto = UserResDto.builder().userId(userId).nickname("테스트유저").build();
        given(userService.getUser(userId)).willReturn(mockDto);

        // when & then
        mockMvc.perform(get("/api/v1/users/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.userId").value(userId));
    }
}
```
