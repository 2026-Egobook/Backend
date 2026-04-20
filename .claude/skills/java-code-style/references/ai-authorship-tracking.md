# AI 작성 추적 규칙

## 케이스 1 — AI 작성/수정 주석 삽입

코드 작성 또는 수정 시, 해당 논리 블록 바로 위에 한 줄 주석을 추가한다.

| 상황 | 형식 |
|---|---|
| 신규 코드 작성 | `// [AI-GEN] {기능 요약 (10단어 이내)}` |
| 기존 코드 수정 | `// [AI-MOD] {변경 내용 요약 (10단어 이내)}` |

**예시:**

```java
// [AI-GEN] 사용자 생성 로직
public UserResDto createUser(String email) { ... }

// [AI-MOD] CustomException으로 예외 처리 방식 통일
User user = userRepository.findById(userId)
        .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));
```

### 위치 기억 규칙 (토큰 최소화)

주석 삽입 시마다 아래 형식으로 내부 목록에 누적한다:

```
파일명:줄번호 | 유형(GEN/MOD)
```

예: `UserService.java:44 | GEN`, `UserService.java:67 | MOD`

- **전체 경로, 코드 내용, 설명은 기억하지 않는다** — 줄번호만으로 충분
- 목록은 모든 코드 작업이 끝날 때까지 유지

---

## 케이스 2 — 작업 완료 후 변경 요약 테이블 출력

모든 코드 작업이 완료되면, 케이스 1에서 누적한 위치 목록을 기반으로 아래 표를 출력한다.

### 출력 형식

| 파일:줄 | 유형 | 변경 내용 | 구현 근거 |
|---|---|---|---|
| `UserService.java:44` | 신규 작성 | createUser() 메서드 | 기존 createUser가 없어 신규 추가 필요 |
| `UserService.java:67` | 수정 | 예외 처리 CustomException으로 변경 | 팀 컨벤션상 모든 예외는 CustomException 사용 |

### 출력 시점 및 효율 규칙

- **출력 시점**: 마지막 코드 작성/수정 응답 직후 자동 출력
- **파일 재탐색 금지**: 이미 세션 컨텍스트에 있는 정보(직전에 작성/수정한 내용)를 그대로 활용
  - 세션 컨텍스트에 없는 경우에만 해당 파일의 해당 줄 ±5줄 범위만 읽는다
- **구현 근거**: 코드 작성 시 판단한 이유를 기술 (컨벤션 준수, 요구사항, 기술 제약 등)
