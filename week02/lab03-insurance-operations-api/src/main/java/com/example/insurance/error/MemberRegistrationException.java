package com.example.insurance.error;

import com.example.insurance.model.MemberModels.ApiError;
import com.example.insurance.model.MemberModels.FieldError;
import java.util.List;

public class MemberRegistrationException extends RuntimeException {
  private final int status;
  private final ApiError error;

  public MemberRegistrationException(int status, String code, String message) {
    this(status, code, message, List.of());
  }

  public MemberRegistrationException(
      int status, String code, String message, List<FieldError> fieldErrors) {
    super(message);
    this.status = status;
    this.error = new ApiError(code, message, List.copyOf(fieldErrors));
  }

  public int status() {
    return status;
  }

  public ApiError error() {
    return error;
  }
}
