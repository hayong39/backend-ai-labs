package com.example.insurance.controller;

import com.example.insurance.error.MemberRegistrationException;
import com.example.insurance.model.MemberModels.ApiError;
import com.example.insurance.model.MemberModels.Member;
import com.example.insurance.model.MemberModels.Registration;
import com.example.insurance.service.MemberService;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/members")
public class MemberController {
  private static final Set<String> FIELDS = Set.of("name", "email", "phoneNumber", "birthDate");
  private final MemberService service;
  private final ObjectMapper mapper;

  public MemberController(MemberService service, ObjectMapper mapper) {
    this.service = service;
    this.mapper = mapper;
  }

  @PostMapping
  public ResponseEntity<Member> register(HttpServletRequest request) {
    checkContentType(request.getContentType());
    JsonNode body;
    try {
      body =
          mapper
              .reader()
              .with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
              .readTree(request.getInputStream());
    } catch (IOException ex) {
      throw invalidRequest();
    }
    if (body == null || !body.isObject()) {
      throw invalidRequest();
    }
    body.fields()
        .forEachRemaining(
            field -> {
              if (!FIELDS.contains(field.getKey())
                  || !(field.getValue().isTextual() || field.getValue().isNull())) {
                throw invalidRequest();
              }
            });
    var registration =
        new Registration(
            body.path("name").textValue(),
            body.path("email").textValue(),
            body.path("phoneNumber").textValue(),
            body.path("birthDate").textValue());
    return ResponseEntity.status(201)
        .contentType(MediaType.APPLICATION_JSON)
        .body(service.register(registration));
  }

  private static void checkContentType(String header) {
    try {
      if (header != null) {
        MediaType type = MediaType.parseMediaType(header);
        if ("application".equalsIgnoreCase(type.getType())
            && "json".equalsIgnoreCase(type.getSubtype())) {
          return;
        }
      }
    } catch (IllegalArgumentException ignored) {
      // Use the same error response for malformed and unsupported media types.
    }
    throw new MemberRegistrationException(
        415, "UNSUPPORTED_MEDIA_TYPE", "Content-Type은 application/json이어야 합니다.");
  }

  private static MemberRegistrationException invalidRequest() {
    return new MemberRegistrationException(400, "INVALID_REQUEST", "요청 JSON 구조를 확인해 주세요.");
  }

  @ExceptionHandler(MemberRegistrationException.class)
  public ResponseEntity<ApiError> registrationError(MemberRegistrationException ex) {
    return ResponseEntity.status(ex.status())
        .contentType(MediaType.APPLICATION_JSON)
        .body(ex.error());
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiError> unexpectedError(Exception ex) {
    return ResponseEntity.internalServerError()
        .contentType(MediaType.APPLICATION_JSON)
        .body(new ApiError("INTERNAL_SERVER_ERROR", "회원 등록 중 오류가 발생했습니다.", List.of()));
  }
}
