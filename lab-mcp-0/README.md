# Lab MCP

```bash
./gradlew :lab-mcp-0:bootRun
```

- 연결: `http://localhost:8082/mcp` (Streamable HTTP)
- `resources/list`로 목록 조회
- `resources/read`에 `{"uri":"context://hello"}`를 전달하면 문자열 반환

## 프로젝트 ID로 개발 가이드 조회

1. `resources/templates/list`로 `context://hello/{id}` 템플릿을 조회한다.
2. 클라이언트에서 `{id}`를 프로젝트 ID `A123`으로 바꾼다.
3. `resources/read`에 `{"uri":"context://hello/A123"}`를 전달한다.
4. 서버는 `A123 프로젝트 개발 가이드`라는 제목과 프로젝트 개발 규칙을 반환한다.

리소스는 [LabMcpApplication.java](src/main/java/com/example/labmcp/LabMcpApplication.java)에 정의되어 있다.
