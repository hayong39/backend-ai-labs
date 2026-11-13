package com.example.insurance.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class MemberModels {
  private MemberModels() {}

  public record Registration(String name, String email, String phoneNumber, String birthDate) {}

  public record Member(
      long id,
      String name,
      String email,
      String phoneNumber,
      LocalDate birthDate,
      Instant createdAt) {}

  public record FieldError(String field, String message) {}

  public record ApiError(String code, String message, List<FieldError> fieldErrors) {}
}
