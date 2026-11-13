package com.example.insurance.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.example.insurance.error.MemberRegistrationException;
import com.example.insurance.model.MemberModels.Registration;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class MemberServiceTest {
  private static final Instant NOW = Instant.parse("2026-11-13T15:30:00Z");
  private final MemberService service = new MemberService(Clock.fixed(NOW, ZoneOffset.UTC));

  private Registration request(String name, String email, String phone, String date) {
    return new Registration(name, email, phone, date);
  }

  @Test
  void acceptsSeoulTodayEvenWhenUtcDateIsYesterday() {
    var member =
        service.register(request("가상회원", "member@example.com", "01000000000", "2026-11-14"));
    assertEquals("2026-11-14", member.birthDate().toString());
    assertEquals(NOW, member.createdAt());
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "2026-11-15",
        "2025-02-29",
        "2026-02-30",
        "",
        "2026-1-01",
        " 2000-01-01",
        "+2000-01-01"
      })
  void rejectsInvalidDatesWithoutReservingIdentity(String date) {
    var exception =
        assertThrows(
            MemberRegistrationException.class,
            () -> service.register(request("가상회원", "member@example.com", "01000000000", date)));
    assertEquals("birthDate", exception.error().fieldErrors().getFirst().field());
    assertEquals(
        1,
        service.register(request("가상회원", "member@example.com", "01000000000", "2000-02-29")).id());
  }

  @Test
  void countsUnicodeCodePointsAndTrimsWhitespace() {
    String name = "😀".repeat(50);
    var member =
        service.register(
            request("\u00a0" + name + "\u2003", " MEMBER@EXAMPLE.COM ", " 010-0000-0000 ", null));
    assertEquals(name, member.name());
    assertEquals("member@example.com", member.email());
    assertEquals("01000000000", member.phoneNumber());
    assertNull(member.birthDate());
    assertThrows(
        MemberRegistrationException.class,
        () -> service.register(request("😀".repeat(51), "other@example.com", "01000000001", null)));
  }

  @Test
  void accepts254CharacterEmailAndRejects255() {
    service.register(request("가상회원", "a".repeat(248) + "@a.com", "01000000000", null));
    var exception =
        assertThrows(
            MemberRegistrationException.class,
            () ->
                service.register(request("가상회원", "a".repeat(249) + "@a.com", "01000000001", null)));
    assertEquals("email", exception.error().fieldErrors().getFirst().field());
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "a@@b.com",
        "@b.com",
        "a@",
        "a@b",
        "a@.b",
        "a@b.",
        "a@b..c",
        "a b@c.com",
        "a@b\u00a0.c"
      })
  void rejectsMalformedEmails(String email) {
    var exception =
        assertThrows(
            MemberRegistrationException.class,
            () -> service.register(request("가상회원", email, "01000000000", null)));
    assertEquals("email", exception.error().fieldErrors().getFirst().field());
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "010 00000000",
        "+821000000000",
        "01100000000",
        "0100000000",
        "010000000000",
        "010００００００００"
      })
  void rejectsMalformedPhones(String phone) {
    var exception =
        assertThrows(
            MemberRegistrationException.class,
            () -> service.register(request("가상회원", "member@example.com", phone, null)));
    assertEquals("phoneNumber", exception.error().fieldErrors().getFirst().field());
  }

  @Test
  void unexpectedFailureBeforeCommitLeavesNoRecordsOrReservedContacts() {
    Clock clock = mock(Clock.class);
    when(clock.instant()).thenThrow(new IllegalStateException("clock failed")).thenReturn(NOW);
    MemberService failing = new MemberService(clock);
    Registration request = request("가상회원", "member@example.com", "01000000000", null);
    assertThrows(IllegalStateException.class, () -> failing.register(request));
    assertEquals(1, failing.register(request).id());
    assertEquals(
        2, failing.register(request("다른회원", "other@example.com", "01000000001", null)).id());
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void concurrentDuplicateRequestsCreateExactlyOneMember(boolean sameEmail) throws Exception {
    int count = 12;
    var ready = new CountDownLatch(count);
    var start = new CountDownLatch(1);
    try (var executor = Executors.newFixedThreadPool(count)) {
      var results = new ArrayList<Future<String>>();
      for (int i = 0; i < count; i++) {
        int index = i;
        results.add(
            executor.submit(
                () -> {
                  ready.countDown();
                  if (!start.await(5, TimeUnit.SECONDS)) throw new AssertionError("start timeout");
                  try {
                    var member =
                        service.register(
                            request(
                                "가상회원",
                                sameEmail ? "same@example.com" : "member" + index + "@example.com",
                                sameEmail ? "010" + String.format("%08d", index) : "01000000000",
                                null));
                    assertEquals(1, member.id());
                    return "CREATED";
                  } catch (MemberRegistrationException ex) {
                    assertEquals(409, ex.status());
                    return ex.error().code();
                  }
                }));
      }
      assertTrue(ready.await(5, TimeUnit.SECONDS));
      start.countDown();
      int successes = 0;
      for (var result : results) {
        String outcome = result.get(5, TimeUnit.SECONDS);
        if (outcome.equals("CREATED")) successes++;
        else assertEquals(sameEmail ? "DUPLICATE_EMAIL" : "DUPLICATE_PHONE_NUMBER", outcome);
      }
      assertEquals(1, successes);
      assertEquals(
          2, service.register(request("다른회원", "other@example.com", "01099999999", null)).id());
    }
  }
}
