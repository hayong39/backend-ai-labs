# Insurance Operations API

보험 계약, 고객, 청구, 납입, 갱신, 상품 정보를 관리하는 Spring Boot 기반 REST API 서비스입니다.

## Environment
- Java 21
- Spring Boot 3.3.5
- Gradle 8.14.3 (Wrapper)
- JUnit 5
- In-memory data storage (no external database required)

## Run

아래 Gradle 명령은 저장소 루트에서 실행한다.

```bash
./gradlew :lab03-insurance-operations-api:bootRun
```

Default port: 8080.

## Test

```bash
./gradlew :lab03-insurance-operations-api:test
```

## API domains
- Customers: customer profiles and preferences
- Contracts: insurance contract lifecycle
- Products: insurance product catalog
- Premiums: premium calculation and quotation
- Payments: payment collection and reconciliation
- Claims: claim submission and review
- Members: member registration (`POST /api/members`)

The service uses in-memory storage and sample records for local execution. Restarting the application resets stored data.

## Source layout
- `controller`: REST endpoints and request validation
- `service`: application operations and business rules
- `model`: request and response models
- `error`: API error responses

For current endpoint details, refer to controller source and request/response models.

## 회원 등록 API

[회원 등록 명세](../../register.md)에 따라 `POST /api/members`를 제공한다.
고객·보험 계약과 별도의 인메모리 회원 저장소를 사용하며, 재시작하면 초기화된다.

```bash
curl -i http://localhost:8080/api/members \
  -H 'Content-Type: application/json' \
  -d '{"name":"가상회원","email":"member@example.com","phoneNumber":"010-0000-0000","birthDate":"1995-06-15"}'
```

성공 시 `201`과 `id`, `name`, `email`, `phoneNumber`, `birthDate`, `createdAt`을 반환한다.
필수 입력·생년월일 검증 실패와 잘못된 JSON은 `400`, 이메일·휴대전화번호 중복은 `409`,
누락되거나 지원하지 않는 Content-Type은 `415`, 예상하지 못한 오류는 `500`이다.
오류는 `code`, `message`, `fieldErrors` JSON 형식이며 개인정보를 포함하지 않는다.
이름·이메일·전화번호를 정규화하며, 생년월일의 오늘 기준은 `Asia/Seoul`이다.
동시에 같은 연락처로 등록해도 한 건만 저장한다. 인증은 교육용 명세에 따라 제공하지 않는다.

```bash
./gradlew :lab03-insurance-operations-api:test --tests '*Member*Test'
```

## OpenAPI / Swagger UI

애플리케이션을 실행한 후 아래 주소에서 API 명세를 확인하고 요청을 실행할 수 있다.

- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs
- OpenAPI YAML: http://localhost:8080/v3/api-docs.yaml

Codespaces에서는 전달된 포트 8080의 브라우저 주소 뒤에 위 경로를 붙인다.
각 실습은 독립 실행하며, 동시에 실행할 때는 서로 다른 서버 포트를 사용한다.

SpringDoc OpenAPI 2.6.0의 WebMVC UI starter를 사용한다.

## JUnit 5 테스트 환경

`spring-boot-starter-test`, `junit-platform-launcher`, `useJUnitPlatform()`을 사용한다.
저장소 루트에서 실행한다.

```bash
./gradlew :lab03-insurance-operations-api:test
```
테스트는 이 실습의 `src/test/java`에 작성한다.
테스트는 이 실습의 `src/test/java`에 작성한다.
보고서: `week02/lab03-insurance-operations-api/build/reports/tests/test/index.html`.
