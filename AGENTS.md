# 에이전트 작업 지침

이 문서는 저장소 전체에 적용한다. 하위 디렉터리에 별도 `AGENTS.md`가 있으면 해당 범위의 지침도 확인한다.

## 프로젝트 개요

- 백엔드 AI 개발 교육용 실습 저장소다. 각 실습은 독립적인 Spring Boot 애플리케이션이다.
- Java 21, Gradle Wrapper 8.14.3, JUnit 5를 사용한다.
- 작업 전 루트와 대상 실습의 `README.md`, 관련 `build.gradle`을 확인한다.
- 사용자에게 작업 결과를 한국어로 설명한다. 코드 식별자는 기존 영어 명명 방식을 따른다.

## 저장소 구조

| 경로 | 역할 | Gradle 프로젝트 |
| --- | --- | --- |
| `week02/lab01-hello-world/` | Hello World REST API | `:lab01-hello-world` |
| `week02/lab02-agent-design-review/` | 보험료 조회 및 할인 정책 실습 | `:lab02-agent-design-review` |
| `week02/lab03-insurance-operations-api/` | 고객·계약·상품·보험료·납입·청구 API | `:lab03-insurance-operations-api` |
| `week02/superpowers/` | 교육용 에이전트 워크플로 및 스크립트 | 해당 없음 |
| `.devcontainer/` | Codespaces / Dev Container 환경 | 해당 없음 |
| `.vscode/` | 공통 편집기 및 포맷 설정 | 해당 없음 |

루트 `settings.gradle`에서 실습 디렉터리와 Gradle 프로젝트를 연결하고, 루트 `build.gradle`에서 공통 포맷 및 전체 검증 작업을 관리한다.

## 실행과 검증

모든 명령은 저장소 루트에서 Gradle Wrapper로 실행한다. Windows 명령 프롬프트에서는 `gradlew.bat`를 사용한다.

```bash
# 환경 확인
java -version
./gradlew --version

# 전체 빌드: 테스트 및 포맷 검사 포함
./gradlew build

# 전체 테스트 / 포맷 검사
./gradlew test
./gradlew spotlessCheck

# 변경한 실습만 검증 (프로젝트 이름을 대상 실습에 맞게 변경)
./gradlew :lab02-agent-design-review:build

# 특정 테스트 실행
./gradlew :lab02-agent-design-review:test --tests '*PremiumServiceTest'

# 변경한 실습의 Java 포맷 적용
./gradlew :lab02-agent-design-review:spotlessApply

# 실습 실행
./gradlew :lab01-hello-world:bootRun
```

- 코드 변경은 대상 실습의 빌드로 검증한다. 공통 Gradle 설정이나 여러 실습에 영향을 주는 변경은 전체 빌드로 검증한다.
- 동작이나 오류를 수정할 때는 해당 사례를 재현하는 JUnit 테스트를 추가하거나 수정한다. 문서만 변경한 경우 애플리케이션 빌드는 생략할 수 있다.
- 테스트는 각 실습의 `src/test/java`에 둔다. HTML 보고서는 각 실습의 `build/reports/tests/test/index.html`에서 확인한다.
- 실행하지 못한 검증은 원인과 함께 명시한다. 기존 실패와 이번 변경으로 발생한 실패를 구분한다.
- 모든 앱의 기본 포트는 8080이다. 동시 실행 시 `--args='--server.port=8081'`처럼 포트를 분리하고, Codespaces에서는 추가 포트도 전달한다.
- 실행 후 `/swagger-ui.html`과 `/v3/api-docs`에서 API를 확인할 수 있다.

## 코드와 설계 규칙

- Java 포맷은 Spotless와 google-java-format을 따른다. 들여쓰기는 공백 2칸이며 수동 스타일 규칙보다 포매터 결과를 기준으로 한다.
- 기존 패키지와 실습별 구조를 유지한다. Lab 03은 `controller`, `service`, `model`, `error`, `config` 구조를 사용한다.
- 컨트롤러는 HTTP 요청·응답 및 검증을 담당하고, 업무 규칙은 서비스나 기존 정책 클래스에서 처리한다.
- API 경로, 응답 필드, HTTP 상태 코드와 보험료 계산 정책은 요구사항에 따라 변경하고 관련 테스트와 문서를 함께 갱신한다.
- Lab 02의 보험료는 정수 단위다. Lab 03의 금액 계산은 기존 `BigDecimal` 사용 방식과 반올림 규칙을 확인한다.
- Lab 02와 Lab 03은 인메모리 데이터를 사용한다. 영속화가 요구되지 않았다면 외부 데이터베이스를 추가하지 않는다.
- 실습마다 Spring Boot 및 SpringDoc 버전이 다르므로 일괄 통일하지 않는다. 의존성 변경은 대상 실습의 BOM과 호환성을 확인한다.

## 변경 범위와 완료 보고

- 요청에 필요한 파일만 수정하고 사용자의 기존 변경을 보존한다. 관련 없는 리팩터링이나 전체 포맷 변경을 섞지 않는다.
- 새 실습을 추가하면 `settings.gradle` 등록과 루트 README의 실습 목록을 함께 갱신한다.
- 개발 환경 변경 시 `.devcontainer/devcontainer.json`, `.devcontainer/Dockerfile`, `.vscode/`의 관련 설정을 확인하고 필요한 컨테이너 재빌드 방법을 안내한다.
- `build/`, `.gradle/` 등 생성물과 캐시, 자격 증명 및 실제 고객 정보는 커밋하지 않는다. 예시는 가상 데이터를 사용한다.
- 완료 시 변경 내용, 실행한 검증과 결과, 남은 제약을 간결하게 보고한다.