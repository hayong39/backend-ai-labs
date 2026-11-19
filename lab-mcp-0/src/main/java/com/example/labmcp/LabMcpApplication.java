package com.example.labmcp;

import io.modelcontextprotocol.spec.McpSchema.ElicitFormRequest;
import io.modelcontextprotocol.spec.McpSchema.ElicitResult;
import io.modelcontextprotocol.spec.McpSchema.ElicitUrlRequest;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.ai.mcp.annotation.McpArg;
import org.springframework.ai.mcp.annotation.McpPrompt;
import org.springframework.ai.mcp.annotation.McpResource;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.context.McpSyncRequestContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@SpringBootApplication
public class LabMcpApplication {

  private final RestClient lab2Client;

  public LabMcpApplication(@Value("${lab2.base-url:http://localhost:8080}") String lab2BaseUrl) {
    var requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(Duration.ofSeconds(3));
    requestFactory.setReadTimeout(Duration.ofSeconds(5));
    this.lab2Client =
        RestClient.builder().baseUrl(lab2BaseUrl).requestFactory(requestFactory).build();
  }

  public static void main(String[] args) {
    SpringApplication.run(LabMcpApplication.class, args);
  }

  @McpResource(uri = "context://hello", name = "hello-context", description = "A123 프로젝트 개발 가이드 문서")
  public String hello() {
    return "프로젝트 개발 규칙: 사용자 요청에 따른다";
  }

  @McpResource(
      uri = "context://hello/{id}",
      name = "hello-project-context",
      description = "프로젝트 ID로 개발 가이드 문서를 조회한다.")
  public String helloProjectContext(String id) {
    return id + " 프로젝트 개발 가이드\n" + hello();
  }

  @McpTool(name = "hello", description = "사용자가 인사하면 반응하는 간단한 도구")
  public String helloTool(McpSyncRequestContext context) {
    var elicitation = context.clientCapabilities().elicitation();
    if (elicitation != null && (elicitation.form() != null || elicitation.url() == null)) {
      Map<String, Object> schema =
          Map.of(
              "type", "object",
              "properties", Map.of("name", Map.of("type", "string", "title", "이름")),
              "required", List.of("name"));
      ElicitResult result =
          context.elicit(ElicitFormRequest.builder("인사에 사용할 이름을 입력해 주세요.", schema).build());

      if (result.action() == ElicitResult.Action.ACCEPT
          && result.content() != null
          && result.content().get("name") instanceof String name
          && !name.isBlank()) {
        return name.strip() + "님, 안녕하세요!";
      }
    }

    return "안녕 안녕";
  }

  public record ConfirmInput(boolean confirm) {}

  @McpTool(
      name = "delete-file",
      description = "파일 삭제 확인 폼을 데모로 표시하고 응답을 반환한다.",
      annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false))
  public String deleteFile(
      @McpArg(name = "path", description = "확인 폼에 표시할 예시 파일 경로", required = true) String path,
      McpSyncRequestContext context) {
    var elicitation = context.clientCapabilities().elicitation();
    if (elicitation == null || (elicitation.form() == null && elicitation.url() != null)) {
      return "이 클라이언트에서는 삭제 확인 폼을 열 수 없습니다.";
    }

    var result = context.elicit(e -> e.message(path + " 파일을 삭제할까요?"), ConfirmInput.class);
    if (result.action() == ElicitResult.Action.ACCEPT
        && result.structuredContent() != null
        && result.structuredContent().confirm()) {
      return "삭제 확인을 받았습니다.";
    }
    return "삭제 확인을 취소했습니다.";
  }

  @McpTool(name = "test-url", description = "외부 인증 페이지를 열도록 사용자에게 요청하는 도구")
  public String getAuthWithUrl(McpSyncRequestContext context) {
    var elicitation = context.clientCapabilities().elicitation();
    if (elicitation == null || elicitation.url() == null) {
      return "이 클라이언트는 URL elicitation을 지원하지 않습니다.";
    }

    var request =
        ElicitUrlRequest.builder(
                "약관을 확인 해 주세요.", "https://example.com/terms", UUID.randomUUID().toString())
            .build();
    var result = context.elicit(request);

    return result.action() == ElicitResult.Action.ACCEPT
        ? "약관 확인 요청을 수락했습니다."
        : "약관 확인 요청을 취소했습니다.";
  }

  @McpPrompt(name = "hello-prompt", description = "간단한 인사말을 생성하는 프롬프트")
  public String helloPrompt() {
    return "사용자에게 한국어로 짧고 친절하게 인사해 주세요.";
  }

  @McpPrompt(name = "code-review", description = "코드 리뷰")
  public String review(
      @McpArg(name = "code", description = "리뷰할 코드", required = true) String code) {
    return "코드 리뷰: " + code;
  }

  @McpResource(uri = "contract://{id}", name = "contract", description = "계약 ID로 계약 조회")
  public String getContract(String id) {
    return "계약 ID: " + id;
  }

  @McpTool(
      name = "get-contract-premium",
      description = "Lab 2의 계약 ID로 보험료와 할인율(%)을 조회한다.",
      annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false),
      generateOutputSchema = true)
  public PremiumQuote getContractPremium(
      @McpArg(name = "id", description = "보험 계약 ID", required = true) long id) {
    try {
      return lab2Client
          .get()
          .uri("/api/contracts/{id}/premium", id)
          .retrieve()
          .requiredBody(PremiumQuote.class);
    } catch (RestClientResponseException e) {
      throw new IllegalStateException(
          switch (e.getStatusCode().value()) {
            case 404 -> "계약을 찾을 수 없습니다. 계약 ID " + id;
            case 400 -> "비활성 계약의 보험료는 조회할 수 없습니다. 계약 ID " + id;
            default -> "Lab 2 보험료 API 조회에 실패했습니다. HTTP " + e.getStatusCode().value();
          },
          e);
    } catch (ResourceAccessException e) {
      throw new IllegalStateException("Lab 2 보험료 API에 연결할 수 없습니다.", e);
    }
  }

  public record PremiumQuote(long contractId, int premium, int discountPercent) {}
}
