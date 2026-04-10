---
name: java-code-style
description: >
  Java / Spring Boot 프로젝트의 코드 작성 시 반드시 이 스킬을 사용하세요.
  Controller, Service, Repository, DTO, Entity, Mapper, Exception, Enums 등 모든 계층의
  코드를 생성하거나 수정할 때, 네이밍 컨벤션, 주석 스타일, 에러 처리, 응답 포맷,
  패키지 구조, 테스트 코드 작성 등 팀 컨벤션을 자동으로 적용합니다.
  "Controller 만들어줘", "Service 로직 짜줘", "DTO 생성해줘", "에러코드 추가해줘" 등
  Spring Boot 관련 코드 작업이라면 무조건 이 스킬을 참조하세요.
---

# Java / Spring Boot 코드 스타일 가이드

## 📁 전체 패키지 구조

```
com.example.project
├── domain
│   └── {도메인명}
│       ├── controller
│       │   ├── XXXControllerDocs.java
│       │   └── XXXController.java
│       ├── service
│       │   └── XXXService.java
│       ├── repository
│       │   ├── XXXRepository.java
│       │   ├── XXXRepositoryCustom.java        # QueryDSL 사용 시
│       │   └── XXXRepositoryCustomImpl.java    # QueryDSL 사용 시
│       ├── dto
│       │   ├── XXXReqDto.java
│       │   └── XXXResDto.java
│       ├── entity
│       │   └── XXX.java
│       ├── mapper
│       │   └── XXXMapper.java
│       ├── exception
│       │   └── XXXErrorCode.java
│       └── enums
│           └── XXXType.java
└── global
    ├── exception
    │   ├── model
    │   │   └── BaseErrorCode.java              # 인터페이스 (변경 금지)
    │   ├── CustomException.java                # (변경 금지)
    │   └── GlobalExceptionHandler.java         # (변경 금지)
    ├── response
    │   ├── GlobalResponse.java                 # 공통 API 응답 포맷 (변경 금지)
    │   └── SliceResponse.java                  # 무한스크롤 응답 포맷 (변경 금지)
    └── enums
        └── GlobalResponseCode.java             # SUCCESS / FAIL (변경 금지)
```

---

## 📖 작업 유형별 참조 파일

작업 요청이 들어오면 아래 표를 보고 해당하는 `references/` 파일을 읽은 뒤 코드를 작성하세요.

| 작업 유형 | 읽어야 할 파일 |
|---|---|
| `Controller`, `ControllerDocs` 생성 또는 수정 | `references/controller.md` |
| `Service` 생성 또는 수정 | `references/service.md` |
| `Repository`, QueryDSL 생성 또는 수정 | `references/repository.md` |
| `DTO`, `Entity`, `Mapper` 생성 또는 수정 | `references/dto-entity-mapper.md` |
| `ErrorCode`, 예외 처리, `GlobalResponse`, `SliceResponse` 관련 | `references/exception-response.md` |
| 테스트 코드 생성 또는 수정 | `references/test.md` |

> 여러 계층에 걸친 작업(예: "User CRUD 전체 만들어줘")은 해당하는 파일을 모두 읽고 작성하세요.

---

## ✅ 공통 체크리스트 (코드 생성 후 반드시 확인)

- [ ] Entity ↔ DTO 변환은 Mapper 클래스를 통해 수행하는가 (DTO 내부 정적 메서드 사용 금지)
- [ ] Controller는 `Docs` 인터페이스와 `구현체` 파일 두 개로 분리되어 있는가
- [ ] Controller 응답이 `GlobalResponse<T>`로 래핑되어 있는가
- [ ] 무한스크롤 API는 `GlobalResponse<SliceResponse<T>>` 형태인가
- [ ] Service 모든 함수에 JavaDoc 주석이 있는가
- [ ] Service 모든 함수에 START / END 로깅이 있는가
- [ ] 예외 발생 시 `throw new CustomException(XXXErrorCode.XXX)` 형식을 쓰는가
- [ ] Repository 커스텀 메서드에 JavaDoc 주석이 있는가
- [ ] DTO는 `record` 형식인가, Response DTO는 `@Builder`가 있는가
- [ ] DTO 네이밍이 `XXXReqDto` / `XXXResDto` 형식인가
- [ ] Entity의 Enum 필드에 `@Enumerated(EnumType.STRING)`이 있는가
- [ ] ErrorCode가 `BaseErrorCode`를 implements 하는가
- [ ] global 하위 파일들을 임의로 수정하지 않았는가
