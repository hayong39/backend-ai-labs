package com.example.insurance.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.insurance.service.MemberService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
class MemberControllerTest {
  private static final AtomicInteger SEQUENCE = new AtomicInteger();
  @Autowired private MockMvc mvc;
  @Autowired private ObjectMapper mapper;
  @SpyBean private MemberService service;
  private Map<String, Object> request;

  @BeforeEach
  void setUp() {
    int number = SEQUENCE.incrementAndGet();
    request = new LinkedHashMap<>();
    request.put("name", "가상회원");
    request.put("email", "member" + number + "@example.com");
    request.put("phoneNumber", "010" + String.format("%08d", number));
  }

  private ResultActions register(Map<String, Object> body) throws Exception {
    return mvc.perform(
        post("/api/members")
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsBytes(body)));
  }

  @Test
  void createsMemberWithNormalizedFieldsAndUtcTimestamp() throws Exception {
    String email = (String) request.get("email");
    String phone = (String) request.get("phoneNumber");
    request.put("name", "  가상 회원  ");
    request.put("email", "  " + email.toUpperCase(java.util.Locale.ROOT) + "  ");
    request.put(
        "phoneNumber",
        phone.substring(0, 3) + "-" + phone.substring(3, 7) + "-" + phone.substring(7));
    request.put("birthDate", "1995-06-15");
    String response =
        register(request)
            .andExpect(status().isCreated())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").isNumber())
            .andExpect(jsonPath("$.name").value("가상 회원"))
            .andExpect(jsonPath("$.email").value(email))
            .andExpect(jsonPath("$.phoneNumber").value(phone))
            .andExpect(jsonPath("$.birthDate").value("1995-06-15"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    org.junit.jupiter.api.Assertions.assertTrue(mapper.readTree(response).get("id").asLong() > 0);
    Instant.parse(mapper.readTree(response).get("createdAt").asText());
  }

  @Test
  void omittedBirthDateIsNullAndCharsetIsAccepted() throws Exception {
    mvc.perform(
            post("/api/members")
                .contentType("application/json; charset=UTF-8")
                .content(mapper.writeValueAsBytes(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.birthDate").value(nullValue()));
  }

  @Test
  void explicitNullBirthDateIsAccepted() throws Exception {
    request.put("birthDate", null);
    register(request)
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.birthDate").value(nullValue()));
  }

  static Stream<Arguments> missingRequiredValues() {
    return Stream.of("name", "email", "phoneNumber")
        .flatMap(
            field ->
                Stream.of("missing", "null", "empty", "blank")
                    .map(kind -> Arguments.of(field, kind)));
  }

  @ParameterizedTest
  @MethodSource("missingRequiredValues")
  void rejectsMissingRequiredValuesWithoutSaving(String field, String kind) throws Exception {
    Map<String, Object> invalid = new LinkedHashMap<>(request);
    switch (kind) {
      case "missing" -> invalid.remove(field);
      case "null" -> invalid.put(field, null);
      case "empty" -> invalid.put(field, "");
      case "blank" -> invalid.put(field, "   ");
      default -> throw new AssertionError(kind);
    }
    register(invalid)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.fieldErrors[0].field").value(field));
    register(request).andExpect(status().isCreated());
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "null", "[]", "42", "\"text\"", "{", "{} {}"})
  void rejectsInvalidJsonStructureWithoutSaving(String json) throws Exception {
    mvc.perform(post("/api/members").contentType(MediaType.APPLICATION_JSON).content(json))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
        .andExpect(jsonPath("$.fieldErrors").isEmpty());
    register(request).andExpect(status().isCreated());
  }

  static Stream<Arguments> nonStringFields() {
    return Stream.of("name", "email", "phoneNumber", "birthDate")
        .flatMap(
            field ->
                Stream.of(42, true, java.util.List.of("text"), Map.of("value", "text"))
                    .map(value -> Arguments.of(field, value)));
  }

  @ParameterizedTest
  @MethodSource("nonStringFields")
  void rejectsCoercionToStrings(String field, Object value) throws Exception {
    Map<String, Object> invalid = new LinkedHashMap<>(request);
    invalid.put(field, value);
    register(invalid)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    register(request).andExpect(status().isCreated());
  }

  @ParameterizedTest
  @ValueSource(strings = {"id", "createdAt", "unknown"})
  void rejectsUnknownFields(String field) throws Exception {
    Map<String, Object> invalid = new LinkedHashMap<>(request);
    invalid.put(field, "unexpected");
    register(invalid)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    register(request).andExpect(status().isCreated());
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "missing",
        "text/plain",
        "application/xml",
        "application/problem+json",
        "application/*"
      })
  void rejectsUnsupportedContentTypesBeforeCheckingBody(String type) throws Exception {
    var builder = post("/api/members").content("{");
    if (!type.equals("missing")) builder.contentType(type);
    mvc.perform(builder)
        .andExpect(status().isUnsupportedMediaType())
        .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"))
        .andExpect(jsonPath("$.fieldErrors").isEmpty());
    register(request).andExpect(status().isCreated());
  }

  @Test
  void duplicateEmailTakesPrecedenceOverPhone() throws Exception {
    register(request).andExpect(status().isCreated());
    request.put(
        "email", " " + ((String) request.get("email")).toUpperCase(java.util.Locale.ROOT) + " ");
    register(request)
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("DUPLICATE_EMAIL"))
        .andExpect(jsonPath("$.fieldErrors").isEmpty());
    request.put("name", "");
    register(request)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
  }

  @Test
  void rejectsNormalizedDuplicatePhone() throws Exception {
    register(request).andExpect(status().isCreated());
    request.put("email", "other" + request.get("email"));
    String phone = (String) request.get("phoneNumber");
    request.put("phoneNumber", "010-" + phone.substring(3, 7) + "-" + phone.substring(7));
    register(request)
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("DUPLICATE_PHONE_NUMBER"));
  }

  @Test
  void collectsAllAvailableFieldErrors() throws Exception {
    request.replaceAll((key, value) -> "");
    request.put("birthDate", "2025-02-30");
    register(request)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.fieldErrors", hasSize(4)));
  }

  @Test
  void unexpectedFailureReturnsSanitizedJsonError() throws Exception {
    doThrow(new IllegalStateException("private member@example.com stack trace"))
        .when(service)
        .register(any());
    register(request)
        .andExpect(status().isInternalServerError())
        .andExpect(
            content()
                .json(
                    """
            {"code":"INTERNAL_SERVER_ERROR","message":"회원 등록 중 오류가 발생했습니다.","fieldErrors":[]}
            """,
                    true));
  }
}
