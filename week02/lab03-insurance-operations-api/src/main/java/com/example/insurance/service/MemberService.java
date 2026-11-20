package com.example.insurance.service;

import com.example.insurance.error.MemberRegistrationException;
import com.example.insurance.model.MemberModels.FieldError;
import com.example.insurance.model.MemberModels.Member;
import com.example.insurance.model.MemberModels.Registration;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class MemberService {
  private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Seoul");
  private final Clock clock;
  private Map<Long, Member> members = Map.of();
  private long lastId;

  public MemberService() {
    this(Clock.systemUTC());
  }

  MemberService(Clock clock) {
    this.clock = clock;
  }

  public synchronized Member register(Registration request) {
    String name = trim(request.name());
    String email = trim(request.email()).toLowerCase(Locale.ROOT);
    String phone = trim(request.phoneNumber()).replace("-", "");
    List<FieldError> errors = new ArrayList<>();
    if (name.isEmpty() || length(name) > 50) {
      errors.add(new FieldError("name", "이름은 1~50자여야 합니다."));
    }
    if (length(email) > 254 || !validEmail(email)) {
      errors.add(new FieldError("email", "올바른 이메일을 입력해 주세요."));
    }
    if (!phone.matches("010[0-9]{8}")) {
      errors.add(new FieldError("phoneNumber", "휴대전화번호는 010으로 시작하는 숫자 11자리여야 합니다."));
    }
    LocalDate birthDate = birthDate(request.birthDate(), errors);
    if (!errors.isEmpty()) {
      throw new MemberRegistrationException(400, "VALIDATION_ERROR", "입력값을 확인해 주세요.", errors);
    }

    if (members.values().stream().anyMatch(m -> m.email().equals(email))) {
      throw new MemberRegistrationException(409, "DUPLICATE_EMAIL", "이미 등록된 이메일입니다.");
    }
    if (members.values().stream().anyMatch(m -> m.phoneNumber().equals(phone))) {
      throw new MemberRegistrationException(409, "DUPLICATE_PHONE_NUMBER", "이미 등록된 휴대전화번호입니다.");
    }

    long id = Math.incrementExact(lastId);
    var member = new Member(id, name, email, phone, birthDate, clock.instant());
    // Publish only a complete snapshot. Failures before publication leave no partial records.
    var updated = new HashMap<>(members);
    updated.put(id, member);
    Map<Long, Member> snapshot = Map.copyOf(updated);
    members = snapshot;
    lastId = id;
    return member;
  }

  private LocalDate birthDate(String value, List<FieldError> errors) {
    if (value == null) {
      return null;
    }
    try {
      if (!value.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")) {
        throw new DateTimeParseException("Invalid format", value, 0);
      }
      LocalDate date = LocalDate.parse(value);
      if (date.isAfter(LocalDate.now(clock.withZone(BUSINESS_ZONE)))) {
        throw new DateTimeParseException("Future date", value, 0);
      }
      return date;
    } catch (DateTimeParseException ex) {
      errors.add(new FieldError("birthDate", "생년월일은 오늘 이전의 유효한 yyyy-MM-dd 날짜여야 합니다."));
      return null;
    }
  }

  private static boolean validEmail(String email) {
    if (email.codePoints().anyMatch(c -> Character.isWhitespace(c) || Character.isSpaceChar(c))) {
      return false;
    }
    int at = email.indexOf('@');
    if (at < 1 || at != email.lastIndexOf('@')) {
      return false;
    }
    String[] labels = email.substring(at + 1).split("\\.", -1);
    return labels.length >= 2 && java.util.Arrays.stream(labels).noneMatch(String::isEmpty);
  }

  private static String trim(String value) {
    if (value == null) {
      return "";
    }
    int start = 0;
    int end = value.length();
    while (start < end && space(value.codePointAt(start))) {
      start += Character.charCount(value.codePointAt(start));
    }
    while (start < end && space(value.codePointBefore(end))) {
      end -= Character.charCount(value.codePointBefore(end));
    }
    return value.substring(start, end);
  }

  private static boolean space(int codePoint) {
    return Character.isWhitespace(codePoint) || Character.isSpaceChar(codePoint);
  }

  private static int length(String value) {
    return value.codePointCount(0, value.length());
  }
}
