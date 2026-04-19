## Egobook BE Codex Guide

> 코드 작성 시 반드시 `java-code-style` 스킬을 참조한다.
> Codex에서는 `C:\Users\nahjj\.codex\skills\java-code-style\SKILL.md` 와
> `C:\Users\nahjj\.codex\skills\java-code-style\references\` 를 기준으로 사용한다.

## Commands

```powershell
.\gradlew.bat build
.\gradlew.bat bootRun --args="--spring.profiles.active=dev"
.\gradlew.bat test
.\gradlew.bat test --tests "com.example.egobook_be.domain.<domain>.<TestClass>"
.\gradlew.bat test --tests "com.example.egobook_be.domain.<domain>.<TestClass>.<method>"
```

## Architecture

**Base package:** `com.example.egobook_be`
**두 루트:** `global/` (공통), `domain/` (기능)

## Key Domains

| 도메인 | 핵심 기술/특이사항 |
|---|---|
| `auth` | JWT + Google OAuth + Redis 블랙리스트 |
| `user` | 프로필, 관리자 API |
| `diary` | 감정 일기 CRUD, PDFBox 내보내기 |
| `ego_room` | 캐릭터 성장, 레벨/능력치 |
| `letters` | 익명 편지, 스케줄러 |
| `psychology` | LangChain4j (gpt-4o-mini / Gemini) |
| `notification` | FCM (Firebase Admin SDK) |
| `shop` | 인앱 아이템, 잉크 재화 |
| `question` | 설문/퀴즈 |
| `home` | 대시보드, 일일 미션 |
| `ads` | AdMob 리워드 콜백 |
| `terms` | 약관 버전 관리 |

## Infrastructure

| | |
|---|---|
| DB | MySQL 8 (AWS RDS) + JPA + QueryDSL 7.0 |
| QueryDSL 생성 경로 | `build/generated/querydsl` |
| Cache | Redis (Docker) |
| Storage | AWS S3 `ap-northeast-2` (버킷명 프로파일별 상이) |
| AI | LangChain4j 0.36.2 |
| Auth | jjwt 0.12.7 + Spring Security + Firebase Admin 9.7.1 |
| API Docs | SpringDoc OpenAPI 2.8.1 (`/swagger-ui`, `/v3/api-docs`) |
| Health | Actuator `/manage/health` |

## Git

**브랜치:** `feature/` · `fix/` · `hotfix/` → `develop` → `main`

**커밋:**

```text
✨ Feat / 🐛 Fix / ♻️ Refactor / 🔧 Settings / 📝 Docs / 🔥 Remove
```

## Tests

| 종류 | 파일명 패턴 | 방식 |
|---|---|---|
| 단위 | `*UnitTest.java` | Service + Mocked Repository |
| 통합 | `*IntegrationTest.java` | `@SpringBootTest` |

**위치:** `src/test/java/com/example/egobook_be/domain/<domain>/`

## Skill Path Rules

스킬 파일을 탐색하거나 수정할 때는 아래 규칙을 따른다.

1. `java-code-style` 스킬은 항상 `C:\Users\nahjj\.codex\skills\java-code-style\SKILL.md` 에서 읽는다.
2. 세부 규칙은 `C:\Users\nahjj\.codex\skills\java-code-style\references\*.md` 에서 읽는다.
3. `.claude/skills/` 와 `~/.claude/skills/` 는 Claude 기준 원본으로만 취급한다.
4. Claude 스킬과 Codex 스킬을 비교하거나 동기화하라는 요청이 있을 때만 `.claude` 경로를 읽는다.

## Output Rules

- 장황한 사전 계획 설명보다 먼저 실행하고, 필요할 때만 짧은 진행 업데이트를 남긴다.
- 코드 변경 내용을 보여줄 때는 전체 파일보다 수정된 블록을 우선한다.
- 완료 후 변경 요약은 가능하면 3줄 이내로 유지한다.
- 자명한 코드에는 주석을 추가하지 않는다.
