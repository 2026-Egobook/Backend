# Exception / 응답 포맷 규칙

## 예외 처리 구조

```
global/exception/
├── model/BaseErrorCode.java       ← 인터페이스 (변경 금지)
├── CustomException.java           ← 커스텀 예외 클래스 (변경 금지)
└── GlobalExceptionHandler.java    ← 전역 예외 처리기 (변경 금지)

{도메인}/exception/
└── XXXErrorCode.java              ← 도메인별 에러코드 (작업 대상)
```

---

## global/exception/model/BaseErrorCode.java (변경 금지)

```java
public interface BaseErrorCode {
    HttpStatus getStatus();
    String getMessage();
}
```

## global/exception/CustomException.java (변경 금지)

```java
@Getter
public class CustomException extends RuntimeException {
    private final BaseErrorCode errorCode;

    public CustomException(BaseErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
```

## global/exception/GlobalExceptionHandler.java (변경 금지)

- `@RestControllerAdvice`로 전역 등록
- `CustomException` → `baseErrorCode`의 status/message로 `GlobalResponse.error()` 반환
- Spring 표준 예외들 (400, 404, 405, 415, 500 등) 모두 핸들링
- **새로운 표준 예외를 추가해야 할 때만 이 파일에 `@ExceptionHandler` 메서드를 추가**

---

## {도메인}/exception/XXXErrorCode.java (작업 대상)

- `BaseErrorCode` implements
- `@Getter`, `@RequiredArgsConstructor` 필수
- `enum` 형식, 한글 메시지 사용
- **예외 발생은 반드시 `throw new CustomException(XXXErrorCode.XXX)` 형식으로**

```java
@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements BaseErrorCode {

    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    USER_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 존재하는 사용자입니다."),
    USER_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "접근 권한이 없습니다.");

    private final HttpStatus httpStatus;
    private final String message;

    @Override
    public HttpStatus getStatus() {
        return this.httpStatus;
    }
}
```

**Service에서 사용하는 방법:**

```java
// 단순 예외
throw new CustomException(UserErrorCode.USER_NOT_FOUND);

// Optional에서 바로 사용
User user = userRepository.findById(userId)
        .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));
```

---

## GlobalResponse (변경 금지)

- 모든 Controller 응답은 반드시 `GlobalResponse<T>`로 래핑
- `record` 형식, 필드 순서: `status → code → message → data`
- `data`가 null이면 JSON 응답에서 `data` 필드 자체가 사라짐 (`@JsonInclude(NON_NULL)`)

```java
// 성공 — 데이터 반환
GlobalResponse.success(data)

// 성공 — 커스텀 메시지 + 데이터 반환
GlobalResponse.success("커스텀 메시지", data)

// 성공 — 상태코드 + 커스텀 메시지 + 데이터 반환
GlobalResponse.success(201, "생성되었습니다.", data)

// 실패 — GlobalExceptionHandler에서 자동 처리, 직접 호출 불필요
GlobalResponse.error(status, message)
```

**Controller에서 사용하는 방법:**

```java
// 단건 조회
return ResponseEntity.ok(GlobalResponse.success(userService.getUser(userId)));

// 생성 (201)
return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(GlobalResponse.success(201, "사용자가 생성되었습니다.", userService.createUser(reqDto)));

// 삭제 (data 없이 메시지만)
return ResponseEntity.ok(GlobalResponse.success("삭제가 완료되었습니다.", null));
```

---

## SliceResponse (변경 금지)

- 무한스크롤 API 응답은 `GlobalResponse<SliceResponse<T>>` 형태로 래핑
- `page`는 프론트 기준 1부터 시작 (내부적으로 `slice.getNumber() + 1`)

```java
// Slice<Dto> → SliceResponse<Dto> (이미 Dto로 변환된 경우)
SliceResponse.of(slice)

// Slice<Entity> → SliceResponse<Dto> (Mapper를 통해 변환)
SliceResponse.of(slice, userMapper::toSearchUserResDto)
```
