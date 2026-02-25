package io.mambatech.mambasplit;

import io.mambatech.mambasplit.security.TokenCodec;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class GroupFlowIT extends ITBase {
  @Autowired private TestRestTemplate rest;
  @Autowired private JdbcTemplate jdbcTemplate;
  private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
    new ParameterizedTypeReference<>() {};
  private static final ParameterizedTypeReference<List<Map<String, Object>>> LIST_OF_MAP_TYPE =
    new ParameterizedTypeReference<>() {};

  @Test
  void groupInviteAcceptAndExpenseFlow() {
    String emailA = "user_" + UUID.randomUUID() + "@example.com";
    String emailB = "user_" + UUID.randomUUID() + "@example.com";
    String emailC = "user_" + UUID.randomUUID() + "@example.com";
    String password = "password123";

    Map<String, Object> signupA = Map.of("email", emailA, "password", password, "displayName", "User A");
    Map<String, Object> signupB = Map.of("email", emailB, "password", password, "displayName", "User B");
    Map<String, Object> signupC = Map.of("email", emailC, "password", password, "displayName", "User C");

    ResponseEntity<Map<String, Object>> signupRespA = rest.exchange(
      "/api/v1/auth/signup",
      HttpMethod.POST,
      new HttpEntity<>(signupA),
      MAP_TYPE
    );
    ResponseEntity<Map<String, Object>> signupRespB = rest.exchange(
      "/api/v1/auth/signup",
      HttpMethod.POST,
      new HttpEntity<>(signupB),
      MAP_TYPE
    );
    ResponseEntity<Map<String, Object>> signupRespC = rest.exchange(
      "/api/v1/auth/signup",
      HttpMethod.POST,
      new HttpEntity<>(signupC),
      MAP_TYPE
    );
    assertThat(signupRespA.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(signupRespB.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(signupRespC.getStatusCode()).isEqualTo(HttpStatus.OK);

    String accessA = (String) signupRespA.getBody().get("accessToken");
    String accessB = (String) signupRespB.getBody().get("accessToken");
    String accessC = (String) signupRespC.getBody().get("accessToken");
    Map<?, ?> userA = (Map<?, ?>) signupRespA.getBody().get("user");
    Map<?, ?> userB = (Map<?, ?>) signupRespB.getBody().get("user");
    Map<?, ?> userC = (Map<?, ?>) signupRespC.getBody().get("user");
    String userIdA = (String) userA.get("id");
    String userIdB = (String) userB.get("id");
    String userIdC = (String) userC.get("id");

    HttpHeaders headersA = new HttpHeaders();
    headersA.setBearerAuth(accessA);
    headersA.setContentType(MediaType.APPLICATION_JSON);

    HttpHeaders headersB = new HttpHeaders();
    headersB.setBearerAuth(accessB);
    headersB.setContentType(MediaType.APPLICATION_JSON);

    HttpHeaders headersC = new HttpHeaders();
    headersC.setBearerAuth(accessC);
    headersC.setContentType(MediaType.APPLICATION_JSON);

    Map<String, Object> createGroup = Map.of("name", "Test Group");
    ResponseEntity<Map<String, Object>> groupResp = rest.exchange(
      "/api/v1/groups",
      HttpMethod.POST,
      new HttpEntity<>(createGroup, headersA),
      MAP_TYPE
    );
    assertThat(groupResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    String groupId = (String) groupResp.getBody().get("id");
    assertThat(groupId).isNotBlank();
    ResponseEntity<List<Map<String, Object>>> groupsForAResp = rest.exchange(
      "/api/v1/groups",
      HttpMethod.GET,
      new HttpEntity<>(headersA),
      new ParameterizedTypeReference<>() {}
    );
    assertThat(groupsForAResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(groupsForAResp.getBody()).extracting(g -> g.get("id")).contains(groupId);

    Map<String, Object> inviteBody = Map.of("email", emailB);
    ResponseEntity<Map<String, Object>> inviteResp = rest.exchange(
      "/api/v1/groups/" + groupId + "/invites",
      HttpMethod.POST,
      new HttpEntity<>(inviteBody, headersA),
      MAP_TYPE
    );
    assertThat(inviteResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    String token = (String) inviteResp.getBody().get("token");
    assertThat(token).isNotBlank();

    ResponseEntity<Map<String, Object>> wrongEmailAcceptResp = rest.exchange(
      "/api/v1/invites/accept",
      HttpMethod.POST,
      new HttpEntity<>(Map.of("token", token), headersC),
      MAP_TYPE
    );
    assertThat(wrongEmailAcceptResp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

    ResponseEntity<Void> acceptResp = rest.exchange(
      "/api/v1/invites/accept",
      HttpMethod.POST,
      new HttpEntity<>(Map.of("token", token), headersB),
      Void.class
    );
    assertThat(acceptResp.getStatusCode()).isEqualTo(HttpStatus.OK);

    ResponseEntity<Map<String, Object>> reuseInviteResp = rest.exchange(
      "/api/v1/invites/accept",
      HttpMethod.POST,
      new HttpEntity<>(Map.of("token", token), headersB),
      MAP_TYPE
    );
    assertThat(reuseInviteResp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

    Map<String, Object> inviteBodyForC = Map.of("email", emailC);
    ResponseEntity<Map<String, Object>> inviteByMemberResp = rest.exchange(
      "/api/v1/groups/" + groupId + "/invites",
      HttpMethod.POST,
      new HttpEntity<>(inviteBodyForC, headersB),
      MAP_TYPE
    );
    assertThat(inviteByMemberResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    String tokenForC = (String) inviteByMemberResp.getBody().get("token");
    assertThat(tokenForC).isNotBlank();

    ResponseEntity<Void> acceptCResp = rest.exchange(
      "/api/v1/invites/accept",
      HttpMethod.POST,
      new HttpEntity<>(Map.of("token", tokenForC), headersC),
      Void.class
    );
    assertThat(acceptCResp.getStatusCode()).isEqualTo(HttpStatus.OK);

    Map<String, Object> createEqual = Map.of(
      "description", "Dinner",
      "payerUserId", userIdA,
      "amountCents", 1000,
      "participants", List.of(userIdA, userIdB, userIdC)
    );

    ResponseEntity<Map<String, Object>> expenseResp = rest.exchange(
      "/api/v1/groups/" + groupId + "/expenses/equal",
      HttpMethod.POST,
      new HttpEntity<>(createEqual, headersA),
      MAP_TYPE
    );
    assertThat(expenseResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat((String) expenseResp.getBody().get("expenseId")).isNotBlank();

    ResponseEntity<Map<String, Object>> detailsResp = rest.exchange(
      "/api/v1/groups/" + groupId + "/details",
      HttpMethod.GET,
      new HttpEntity<>(headersB),
      MAP_TYPE
    );
    assertThat(detailsResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    Map<String, Object> detailsBody = detailsResp.getBody();
    assertThat(detailsBody).isNotNull();

    Map<String, Object> group = (Map<String, Object>) detailsBody.get("group");
    assertThat(group.get("id")).isEqualTo(groupId);
    assertThat(group.get("name")).isEqualTo("Test Group");

    Map<String, Object> me = (Map<String, Object>) detailsBody.get("me");
    assertThat(me.get("userId")).isEqualTo(userIdB);
    assertThat(me.get("role")).isEqualTo("MEMBER");
    long meNet = ((Number) me.get("netBalanceCents")).longValue();
    assertThat(meNet).isLessThan(0L);

    List<Map<String, Object>> members = (List<Map<String, Object>>) detailsBody.get("members");
    assertThat(members).hasSize(3);
    assertThat(members).extracting(m -> m.get("userId")).containsExactlyInAnyOrder(userIdA, userIdB, userIdC);

    List<Map<String, Object>> expenses = (List<Map<String, Object>>) detailsBody.get("expenses");
    assertThat(expenses).hasSize(1);
    assertThat(expenses.get(0).get("description")).isEqualTo("Dinner");
    assertThat(((Number) expenses.get(0).get("amountCents")).longValue()).isEqualTo(1000L);
    List<Map<String, Object>> splits = (List<Map<String, Object>>) expenses.get(0).get("splits");
    long userBOwed = splits.stream()
      .filter(s -> userIdB.equals(s.get("userId")))
      .mapToLong(s -> ((Number) s.get("amountOwedCents")).longValue())
      .findFirst()
      .orElseThrow();
    assertThat(meNet).isEqualTo(-userBOwed);

    Map<String, Object> summary = (Map<String, Object>) detailsBody.get("summary");
    assertThat(((Number) summary.get("expenseCount")).intValue()).isEqualTo(1);
    assertThat(((Number) summary.get("totalExpenseAmountCents")).longValue()).isEqualTo(1000L);
  }

  @Test
  void groupOwnerCanDeleteButMemberCannot() {
    String emailA = "user_" + UUID.randomUUID() + "@example.com";
    String emailB = "user_" + UUID.randomUUID() + "@example.com";
    String password = "password123";

    Map<String, Object> signupA = Map.of("email", emailA, "password", password, "displayName", "Owner");
    Map<String, Object> signupB = Map.of("email", emailB, "password", password, "displayName", "Member");

    ResponseEntity<Map<String, Object>> signupRespA = rest.exchange(
      "/api/v1/auth/signup",
      HttpMethod.POST,
      new HttpEntity<>(signupA),
      MAP_TYPE
    );
    ResponseEntity<Map<String, Object>> signupRespB = rest.exchange(
      "/api/v1/auth/signup",
      HttpMethod.POST,
      new HttpEntity<>(signupB),
      MAP_TYPE
    );
    assertThat(signupRespA.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(signupRespB.getStatusCode()).isEqualTo(HttpStatus.OK);

    String accessA = (String) signupRespA.getBody().get("accessToken");
    String accessB = (String) signupRespB.getBody().get("accessToken");

    HttpHeaders headersA = new HttpHeaders();
    headersA.setBearerAuth(accessA);
    headersA.setContentType(MediaType.APPLICATION_JSON);

    HttpHeaders headersB = new HttpHeaders();
    headersB.setBearerAuth(accessB);
    headersB.setContentType(MediaType.APPLICATION_JSON);

    ResponseEntity<Map<String, Object>> groupResp = rest.exchange(
      "/api/v1/groups",
      HttpMethod.POST,
      new HttpEntity<>(Map.of("name", "Delete Group"), headersA),
      MAP_TYPE
    );
    assertThat(groupResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    String groupId = (String) groupResp.getBody().get("id");

    ResponseEntity<Map<String, Object>> inviteResp = rest.exchange(
      "/api/v1/groups/" + groupId + "/invites",
      HttpMethod.POST,
      new HttpEntity<>(Map.of("email", emailB), headersA),
      MAP_TYPE
    );
    assertThat(inviteResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    String token = (String) inviteResp.getBody().get("token");

    ResponseEntity<Void> acceptResp = rest.exchange(
      "/api/v1/invites/accept",
      HttpMethod.POST,
      new HttpEntity<>(Map.of("token", token), headersB),
      Void.class
    );
    assertThat(acceptResp.getStatusCode()).isEqualTo(HttpStatus.OK);

    ResponseEntity<Map<String, Object>> memberDeleteResp = rest.exchange(
      "/api/v1/groups/" + groupId,
      HttpMethod.DELETE,
      new HttpEntity<>(headersB),
      MAP_TYPE
    );
    assertThat(memberDeleteResp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

    ResponseEntity<Void> ownerDeleteResp = rest.exchange(
      "/api/v1/groups/" + groupId,
      HttpMethod.DELETE,
      new HttpEntity<>(headersA),
      Void.class
    );
    assertThat(ownerDeleteResp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

    ResponseEntity<List<Map<String, Object>>> groupsForOwnerResp = rest.exchange(
      "/api/v1/groups",
      HttpMethod.GET,
      new HttpEntity<>(headersA),
      new ParameterizedTypeReference<>() {}
    );
    ResponseEntity<List<Map<String, Object>>> groupsForMemberResp = rest.exchange(
      "/api/v1/groups",
      HttpMethod.GET,
      new HttpEntity<>(headersB),
      new ParameterizedTypeReference<>() {}
    );
    assertThat(groupsForOwnerResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(groupsForMemberResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(groupsForOwnerResp.getBody()).extracting(g -> g.get("id")).doesNotContain(groupId);
    assertThat(groupsForMemberResp.getBody()).extracting(g -> g.get("id")).doesNotContain(groupId);
  }

  @Test
  void expenseOwnerCanDeleteButNonOwnerCannot() {
    String emailA = "user_" + UUID.randomUUID() + "@example.com";
    String emailB = "user_" + UUID.randomUUID() + "@example.com";
    String password = "password123";

    ResponseEntity<Map<String, Object>> signupRespA = rest.exchange(
      "/api/v1/auth/signup",
      HttpMethod.POST,
      new HttpEntity<>(Map.of("email", emailA, "password", password, "displayName", "Owner")),
      MAP_TYPE
    );
    ResponseEntity<Map<String, Object>> signupRespB = rest.exchange(
      "/api/v1/auth/signup",
      HttpMethod.POST,
      new HttpEntity<>(Map.of("email", emailB, "password", password, "displayName", "Member")),
      MAP_TYPE
    );
    assertThat(signupRespA.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(signupRespB.getStatusCode()).isEqualTo(HttpStatus.OK);

    String accessA = (String) signupRespA.getBody().get("accessToken");
    String accessB = (String) signupRespB.getBody().get("accessToken");
    String userIdA = (String) ((Map<?, ?>) signupRespA.getBody().get("user")).get("id");
    String userIdB = (String) ((Map<?, ?>) signupRespB.getBody().get("user")).get("id");

    HttpHeaders headersA = new HttpHeaders();
    headersA.setBearerAuth(accessA);
    headersA.setContentType(MediaType.APPLICATION_JSON);

    HttpHeaders headersB = new HttpHeaders();
    headersB.setBearerAuth(accessB);
    headersB.setContentType(MediaType.APPLICATION_JSON);

    ResponseEntity<Map<String, Object>> groupResp = rest.exchange(
      "/api/v1/groups",
      HttpMethod.POST,
      new HttpEntity<>(Map.of("name", "Delete Expense"), headersA),
      MAP_TYPE
    );
    assertThat(groupResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    String groupId = (String) groupResp.getBody().get("id");

    ResponseEntity<Map<String, Object>> inviteResp = rest.exchange(
      "/api/v1/groups/" + groupId + "/invites",
      HttpMethod.POST,
      new HttpEntity<>(Map.of("email", emailB), headersA),
      MAP_TYPE
    );
    assertThat(inviteResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    String token = (String) inviteResp.getBody().get("token");

    ResponseEntity<Void> acceptResp = rest.exchange(
      "/api/v1/invites/accept",
      HttpMethod.POST,
      new HttpEntity<>(Map.of("token", token), headersB),
      Void.class
    );
    assertThat(acceptResp.getStatusCode()).isEqualTo(HttpStatus.OK);

    ResponseEntity<Map<String, Object>> createExpenseResp = rest.exchange(
      "/api/v1/groups/" + groupId + "/expenses/equal",
      HttpMethod.POST,
      new HttpEntity<>(Map.of(
        "description", "Lunch",
        "payerUserId", userIdA,
        "amountCents", 1200,
        "participants", List.of(userIdA, userIdB)
      ), headersA),
      MAP_TYPE
    );
    assertThat(createExpenseResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    String expenseId = (String) createExpenseResp.getBody().get("expenseId");

    ResponseEntity<Map<String, Object>> memberDeleteResp = rest.exchange(
      "/api/v1/groups/" + groupId + "/expenses/" + expenseId,
      HttpMethod.DELETE,
      new HttpEntity<>(headersB),
      MAP_TYPE
    );
    assertThat(memberDeleteResp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

    ResponseEntity<Void> ownerDeleteResp = rest.exchange(
      "/api/v1/groups/" + groupId + "/expenses/" + expenseId,
      HttpMethod.DELETE,
      new HttpEntity<>(headersA),
      Void.class
    );
    assertThat(ownerDeleteResp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

    ResponseEntity<Map<String, Object>> detailsResp = rest.exchange(
      "/api/v1/groups/" + groupId + "/details",
      HttpMethod.GET,
      new HttpEntity<>(headersA),
      MAP_TYPE
    );
    assertThat(detailsResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    Map<String, Object> summary = (Map<String, Object>) detailsResp.getBody().get("summary");
    List<Map<String, Object>> expenses = (List<Map<String, Object>>) detailsResp.getBody().get("expenses");
    assertThat(summary.get("expenseCount")).isEqualTo(0);
    assertThat(((Number) summary.get("totalExpenseAmountCents")).longValue()).isEqualTo(0L);
    assertThat(expenses).isEmpty();
  }

  @Test
  void memberCanCancelInviteAndCanceledInviteCannotBeAccepted() {
    String emailA = "user_" + UUID.randomUUID() + "@example.com";
    String emailB = "user_" + UUID.randomUUID() + "@example.com";
    String emailC = "user_" + UUID.randomUUID() + "@example.com";
    String password = "password123";

    ResponseEntity<Map<String, Object>> signupRespA = rest.exchange(
      "/api/v1/auth/signup",
      HttpMethod.POST,
      new HttpEntity<>(Map.of("email", emailA, "password", password, "displayName", "Owner")),
      MAP_TYPE
    );
    ResponseEntity<Map<String, Object>> signupRespB = rest.exchange(
      "/api/v1/auth/signup",
      HttpMethod.POST,
      new HttpEntity<>(Map.of("email", emailB, "password", password, "displayName", "Invitee")),
      MAP_TYPE
    );
    ResponseEntity<Map<String, Object>> signupRespC = rest.exchange(
      "/api/v1/auth/signup",
      HttpMethod.POST,
      new HttpEntity<>(Map.of("email", emailC, "password", password, "displayName", "Outsider")),
      MAP_TYPE
    );
    assertThat(signupRespA.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(signupRespB.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(signupRespC.getStatusCode()).isEqualTo(HttpStatus.OK);

    String accessA = (String) signupRespA.getBody().get("accessToken");
    String accessB = (String) signupRespB.getBody().get("accessToken");
    String accessC = (String) signupRespC.getBody().get("accessToken");

    HttpHeaders headersA = new HttpHeaders();
    headersA.setBearerAuth(accessA);
    headersA.setContentType(MediaType.APPLICATION_JSON);

    HttpHeaders headersB = new HttpHeaders();
    headersB.setBearerAuth(accessB);
    headersB.setContentType(MediaType.APPLICATION_JSON);

    HttpHeaders headersC = new HttpHeaders();
    headersC.setBearerAuth(accessC);
    headersC.setContentType(MediaType.APPLICATION_JSON);

    ResponseEntity<Map<String, Object>> groupResp = rest.exchange(
      "/api/v1/groups",
      HttpMethod.POST,
      new HttpEntity<>(Map.of("name", "Cancelable Invite"), headersA),
      MAP_TYPE
    );
    assertThat(groupResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    String groupId = (String) groupResp.getBody().get("id");

    ResponseEntity<Map<String, Object>> inviteResp = rest.exchange(
      "/api/v1/groups/" + groupId + "/invites",
      HttpMethod.POST,
      new HttpEntity<>(Map.of("email", emailB), headersA),
      MAP_TYPE
    );
    assertThat(inviteResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    String token = (String) inviteResp.getBody().get("token");

    ResponseEntity<Map<String, Object>> outsiderCancelResp = rest.exchange(
      "/api/v1/groups/" + groupId + "/invites/" + token,
      HttpMethod.DELETE,
      new HttpEntity<>(headersC),
      MAP_TYPE
    );
    assertThat(outsiderCancelResp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

    ResponseEntity<Void> cancelResp = rest.exchange(
      "/api/v1/groups/" + groupId + "/invites/" + token,
      HttpMethod.DELETE,
      new HttpEntity<>(headersA),
      Void.class
    );
    assertThat(cancelResp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

    ResponseEntity<Map<String, Object>> acceptAfterCancelResp = rest.exchange(
      "/api/v1/invites/accept",
      HttpMethod.POST,
      new HttpEntity<>(Map.of("token", token), headersB),
      MAP_TYPE
    );
    assertThat(acceptAfterCancelResp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

    ResponseEntity<Map<String, Object>> secondCancelResp = rest.exchange(
      "/api/v1/groups/" + groupId + "/invites/" + token,
      HttpMethod.DELETE,
      new HttpEntity<>(headersA),
      MAP_TYPE
    );
    assertThat(secondCancelResp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void listPendingInvitesReturnsOnlyMatchingUserPendingInvitesNewestFirst() {
    String emailA = "owner_" + UUID.randomUUID() + "@example.com";
    String emailB = "invitee_" + UUID.randomUUID() + "@example.com";
    String password = "password123";

    ResponseEntity<Map<String, Object>> signupRespA = rest.exchange(
      "/api/v1/auth/signup",
      HttpMethod.POST,
      new HttpEntity<>(Map.of("email", emailA, "password", password, "displayName", "Owner")),
      MAP_TYPE
    );
    ResponseEntity<Map<String, Object>> signupRespB = rest.exchange(
      "/api/v1/auth/signup",
      HttpMethod.POST,
      new HttpEntity<>(Map.of("email", emailB, "password", password, "displayName", "Invitee")),
      MAP_TYPE
    );
    assertThat(signupRespA.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(signupRespB.getStatusCode()).isEqualTo(HttpStatus.OK);

    String accessA = (String) signupRespA.getBody().get("accessToken");
    String accessB = (String) signupRespB.getBody().get("accessToken");
    String normalizedEmailB = emailB.trim().toLowerCase();

    HttpHeaders headersA = new HttpHeaders();
    headersA.setBearerAuth(accessA);
    headersA.setContentType(MediaType.APPLICATION_JSON);

    HttpHeaders headersB = new HttpHeaders();
    headersB.setBearerAuth(accessB);

    ResponseEntity<Map<String, Object>> group1Resp = rest.exchange(
      "/api/v1/groups",
      HttpMethod.POST,
      new HttpEntity<>(Map.of("name", "Trip A"), headersA),
      MAP_TYPE
    );
    ResponseEntity<Map<String, Object>> group2Resp = rest.exchange(
      "/api/v1/groups",
      HttpMethod.POST,
      new HttpEntity<>(Map.of("name", "Trip B"), headersA),
      MAP_TYPE
    );
    assertThat(group1Resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(group2Resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    String groupId1 = (String) group1Resp.getBody().get("id");
    String groupId2 = (String) group2Resp.getBody().get("id");

    ResponseEntity<Map<String, Object>> invite1Resp = rest.exchange(
      "/api/v1/groups/" + groupId1 + "/invites",
      HttpMethod.POST,
      new HttpEntity<>(Map.of("email", emailB), headersA),
      MAP_TYPE
    );
    ResponseEntity<Map<String, Object>> invite2Resp = rest.exchange(
      "/api/v1/groups/" + groupId2 + "/invites",
      HttpMethod.POST,
      new HttpEntity<>(Map.of("email", emailB.toUpperCase()), headersA),
      MAP_TYPE
    );
    assertThat(invite1Resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(invite2Resp.getStatusCode()).isEqualTo(HttpStatus.OK);

    ResponseEntity<List<Map<String, Object>>> listResp = rest.exchange(
      "/api/v1/invites?email=" + normalizedEmailB,
      HttpMethod.GET,
      new HttpEntity<>(headersB),
      LIST_OF_MAP_TYPE
    );
    assertThat(listResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(listResp.getBody()).hasSize(2);

    List<Map<String, Object>> invites = listResp.getBody();
    assertThat(invites).extracting(i -> i.get("groupName")).containsExactly("Trip B", "Trip A");
    assertThat(invites).allSatisfy(invite -> {
      assertThat(invite).containsKeys("id", "groupId", "groupName", "email", "expiresAt", "createdAt");
      assertThat(invite).doesNotContainKeys("tokenHash", "token");
      assertThat((String) invite.get("email")).isEqualTo(normalizedEmailB);
    });

    Instant createdAt0 = Instant.parse((String) invites.get(0).get("createdAt"));
    Instant createdAt1 = Instant.parse((String) invites.get(1).get("createdAt"));
    assertThat(createdAt0).isAfterOrEqualTo(createdAt1);
  }

  @Test
  void listPendingInvitesReturnsEmptyWhenNoneExist() {
    String email = "user_" + UUID.randomUUID() + "@example.com";
    String password = "password123";

    ResponseEntity<Map<String, Object>> signupResp = rest.exchange(
      "/api/v1/auth/signup",
      HttpMethod.POST,
      new HttpEntity<>(Map.of("email", email, "password", password, "displayName", "User")),
      MAP_TYPE
    );
    assertThat(signupResp.getStatusCode()).isEqualTo(HttpStatus.OK);

    String access = (String) signupResp.getBody().get("accessToken");
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(access);

    ResponseEntity<List<Map<String, Object>>> listResp = rest.exchange(
      "/api/v1/invites?email=" + email.toLowerCase(),
      HttpMethod.GET,
      new HttpEntity<>(headers),
      LIST_OF_MAP_TYPE
    );
    assertThat(listResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(listResp.getBody()).isEmpty();
  }

  @Test
  void listPendingInvitesForbiddenWhenQueryEmailDoesNotMatchAuthenticatedUser() {
    String email = "user_" + UUID.randomUUID() + "@example.com";
    String otherEmail = "other_" + UUID.randomUUID() + "@example.com";
    String password = "password123";

    ResponseEntity<Map<String, Object>> signupResp = rest.exchange(
      "/api/v1/auth/signup",
      HttpMethod.POST,
      new HttpEntity<>(Map.of("email", email, "password", password, "displayName", "User")),
      MAP_TYPE
    );
    assertThat(signupResp.getStatusCode()).isEqualTo(HttpStatus.OK);

    String access = (String) signupResp.getBody().get("accessToken");
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(access);

    ResponseEntity<Map<String, Object>> listResp = rest.exchange(
      "/api/v1/invites?email=" + otherEmail,
      HttpMethod.GET,
      new HttpEntity<>(headers),
      MAP_TYPE
    );
    assertThat(listResp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  void listPendingInvitesExcludesExpiredInvites() {
    String emailA = "owner_" + UUID.randomUUID() + "@example.com";
    String emailB = "invitee_" + UUID.randomUUID() + "@example.com";
    String password = "password123";

    ResponseEntity<Map<String, Object>> signupRespA = rest.exchange(
      "/api/v1/auth/signup",
      HttpMethod.POST,
      new HttpEntity<>(Map.of("email", emailA, "password", password, "displayName", "Owner")),
      MAP_TYPE
    );
    ResponseEntity<Map<String, Object>> signupRespB = rest.exchange(
      "/api/v1/auth/signup",
      HttpMethod.POST,
      new HttpEntity<>(Map.of("email", emailB, "password", password, "displayName", "Invitee")),
      MAP_TYPE
    );
    assertThat(signupRespA.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(signupRespB.getStatusCode()).isEqualTo(HttpStatus.OK);

    String accessA = (String) signupRespA.getBody().get("accessToken");
    String accessB = (String) signupRespB.getBody().get("accessToken");

    HttpHeaders headersA = new HttpHeaders();
    headersA.setBearerAuth(accessA);
    headersA.setContentType(MediaType.APPLICATION_JSON);

    HttpHeaders headersB = new HttpHeaders();
    headersB.setBearerAuth(accessB);

    ResponseEntity<Map<String, Object>> groupResp = rest.exchange(
      "/api/v1/groups",
      HttpMethod.POST,
      new HttpEntity<>(Map.of("name", "Expire Me"), headersA),
      MAP_TYPE
    );
    assertThat(groupResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    String groupId = (String) groupResp.getBody().get("id");

    ResponseEntity<Map<String, Object>> inviteResp = rest.exchange(
      "/api/v1/groups/" + groupId + "/invites",
      HttpMethod.POST,
      new HttpEntity<>(Map.of("email", emailB), headersA),
      MAP_TYPE
    );
    assertThat(inviteResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    String token = (String) inviteResp.getBody().get("token");

    String tokenHash = TokenCodec.sha256Base64Url(token);
    int updated = jdbcTemplate.update(
      "UPDATE invites SET expires_at = now() - interval '1 minute' WHERE token_hash = ?",
      tokenHash
    );
    assertThat(updated).isEqualTo(1);

    ResponseEntity<List<Map<String, Object>>> listResp = rest.exchange(
      "/api/v1/invites?email=" + emailB.toLowerCase(),
      HttpMethod.GET,
      new HttpEntity<>(headersB),
      LIST_OF_MAP_TYPE
    );
    assertThat(listResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(listResp.getBody()).isEmpty();
  }

  @Test
  void listPendingInvitesExcludesInvitesFromDeletedGroups() {
    String emailA = "owner_" + UUID.randomUUID() + "@example.com";
    String emailB = "invitee_" + UUID.randomUUID() + "@example.com";
    String password = "password123";

    ResponseEntity<Map<String, Object>> signupRespA = rest.exchange(
      "/api/v1/auth/signup",
      HttpMethod.POST,
      new HttpEntity<>(Map.of("email", emailA, "password", password, "displayName", "Owner")),
      MAP_TYPE
    );
    ResponseEntity<Map<String, Object>> signupRespB = rest.exchange(
      "/api/v1/auth/signup",
      HttpMethod.POST,
      new HttpEntity<>(Map.of("email", emailB, "password", password, "displayName", "Invitee")),
      MAP_TYPE
    );
    assertThat(signupRespA.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(signupRespB.getStatusCode()).isEqualTo(HttpStatus.OK);

    String accessA = (String) signupRespA.getBody().get("accessToken");
    String accessB = (String) signupRespB.getBody().get("accessToken");

    HttpHeaders headersA = new HttpHeaders();
    headersA.setBearerAuth(accessA);
    headersA.setContentType(MediaType.APPLICATION_JSON);

    HttpHeaders headersB = new HttpHeaders();
    headersB.setBearerAuth(accessB);

    ResponseEntity<Map<String, Object>> groupResp = rest.exchange(
      "/api/v1/groups",
      HttpMethod.POST,
      new HttpEntity<>(Map.of("name", "Delete Invite Group"), headersA),
      MAP_TYPE
    );
    assertThat(groupResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    String groupId = (String) groupResp.getBody().get("id");

    ResponseEntity<Map<String, Object>> inviteResp = rest.exchange(
      "/api/v1/groups/" + groupId + "/invites",
      HttpMethod.POST,
      new HttpEntity<>(Map.of("email", emailB), headersA),
      MAP_TYPE
    );
    assertThat(inviteResp.getStatusCode()).isEqualTo(HttpStatus.OK);

    ResponseEntity<Void> deleteGroupResp = rest.exchange(
      "/api/v1/groups/" + groupId,
      HttpMethod.DELETE,
      new HttpEntity<>(headersA),
      Void.class
    );
    assertThat(deleteGroupResp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

    ResponseEntity<List<Map<String, Object>>> listResp = rest.exchange(
      "/api/v1/invites?email=" + emailB.toLowerCase(),
      HttpMethod.GET,
      new HttpEntity<>(headersB),
      LIST_OF_MAP_TYPE
    );
    assertThat(listResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(listResp.getBody()).isEmpty();
  }

  @Test
  void cannotCreateDuplicatePendingInviteForSameGroupAndEmail() {
    String ownerEmail = "owner_" + UUID.randomUUID() + "@example.com";
    String inviteeEmail = "invitee_" + UUID.randomUUID() + "@example.com";
    String password = "password123";

    ResponseEntity<Map<String, Object>> signupOwnerResp = rest.exchange(
      "/api/v1/auth/signup",
      HttpMethod.POST,
      new HttpEntity<>(Map.of("email", ownerEmail, "password", password, "displayName", "Owner")),
      MAP_TYPE
    );
    assertThat(signupOwnerResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    String ownerAccess = (String) signupOwnerResp.getBody().get("accessToken");

    HttpHeaders ownerHeaders = new HttpHeaders();
    ownerHeaders.setBearerAuth(ownerAccess);
    ownerHeaders.setContentType(MediaType.APPLICATION_JSON);

    ResponseEntity<Map<String, Object>> groupResp = rest.exchange(
      "/api/v1/groups",
      HttpMethod.POST,
      new HttpEntity<>(Map.of("name", "No Duplicates"), ownerHeaders),
      MAP_TYPE
    );
    assertThat(groupResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    String groupId = (String) groupResp.getBody().get("id");

    ResponseEntity<Map<String, Object>> firstInviteResp = rest.exchange(
      "/api/v1/groups/" + groupId + "/invites",
      HttpMethod.POST,
      new HttpEntity<>(Map.of("email", inviteeEmail), ownerHeaders),
      MAP_TYPE
    );
    assertThat(firstInviteResp.getStatusCode()).isEqualTo(HttpStatus.OK);

    ResponseEntity<Map<String, Object>> duplicateInviteResp = rest.exchange(
      "/api/v1/groups/" + groupId + "/invites",
      HttpMethod.POST,
      new HttpEntity<>(Map.of("email", inviteeEmail.toUpperCase()), ownerHeaders),
      MAP_TYPE
    );
    assertThat(duplicateInviteResp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
  }

  @Test
  void cannotInviteUserAlreadyInGroup() {
    String ownerEmail = "owner_" + UUID.randomUUID() + "@example.com";
    String memberEmail = "member_" + UUID.randomUUID() + "@example.com";
    String password = "password123";

    ResponseEntity<Map<String, Object>> signupOwnerResp = rest.exchange(
      "/api/v1/auth/signup",
      HttpMethod.POST,
      new HttpEntity<>(Map.of("email", ownerEmail, "password", password, "displayName", "Owner")),
      MAP_TYPE
    );
    ResponseEntity<Map<String, Object>> signupMemberResp = rest.exchange(
      "/api/v1/auth/signup",
      HttpMethod.POST,
      new HttpEntity<>(Map.of("email", memberEmail, "password", password, "displayName", "Member")),
      MAP_TYPE
    );
    assertThat(signupOwnerResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(signupMemberResp.getStatusCode()).isEqualTo(HttpStatus.OK);

    String ownerAccess = (String) signupOwnerResp.getBody().get("accessToken");
    String memberAccess = (String) signupMemberResp.getBody().get("accessToken");

    HttpHeaders ownerHeaders = new HttpHeaders();
    ownerHeaders.setBearerAuth(ownerAccess);
    ownerHeaders.setContentType(MediaType.APPLICATION_JSON);

    HttpHeaders memberHeaders = new HttpHeaders();
    memberHeaders.setBearerAuth(memberAccess);
    memberHeaders.setContentType(MediaType.APPLICATION_JSON);

    ResponseEntity<Map<String, Object>> groupResp = rest.exchange(
      "/api/v1/groups",
      HttpMethod.POST,
      new HttpEntity<>(Map.of("name", "No Member Reinvite"), ownerHeaders),
      MAP_TYPE
    );
    assertThat(groupResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    String groupId = (String) groupResp.getBody().get("id");

    ResponseEntity<Map<String, Object>> inviteResp = rest.exchange(
      "/api/v1/groups/" + groupId + "/invites",
      HttpMethod.POST,
      new HttpEntity<>(Map.of("email", memberEmail), ownerHeaders),
      MAP_TYPE
    );
    assertThat(inviteResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    String token = (String) inviteResp.getBody().get("token");

    ResponseEntity<Void> acceptResp = rest.exchange(
      "/api/v1/invites/accept",
      HttpMethod.POST,
      new HttpEntity<>(Map.of("token", token), memberHeaders),
      Void.class
    );
    assertThat(acceptResp.getStatusCode()).isEqualTo(HttpStatus.OK);

    ResponseEntity<Map<String, Object>> reinviteResp = rest.exchange(
      "/api/v1/groups/" + groupId + "/invites",
      HttpMethod.POST,
      new HttpEntity<>(Map.of("email", memberEmail), ownerHeaders),
      MAP_TYPE
    );
    assertThat(reinviteResp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

    ResponseEntity<Map<String, Object>> selfInviteResp = rest.exchange(
      "/api/v1/groups/" + groupId + "/invites",
      HttpMethod.POST,
      new HttpEntity<>(Map.of("email", ownerEmail), ownerHeaders),
      MAP_TYPE
    );
    assertThat(selfInviteResp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
  }

  @Test
  void inviteeCanAcceptPendingInviteById() {
    String emailA = "owner_" + UUID.randomUUID() + "@example.com";
    String emailB = "invitee_" + UUID.randomUUID() + "@example.com";
    String password = "password123";

    ResponseEntity<Map<String, Object>> signupRespA = rest.exchange(
      "/api/v1/auth/signup",
      HttpMethod.POST,
      new HttpEntity<>(Map.of("email", emailA, "password", password, "displayName", "Owner")),
      MAP_TYPE
    );
    ResponseEntity<Map<String, Object>> signupRespB = rest.exchange(
      "/api/v1/auth/signup",
      HttpMethod.POST,
      new HttpEntity<>(Map.of("email", emailB, "password", password, "displayName", "Invitee")),
      MAP_TYPE
    );
    assertThat(signupRespA.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(signupRespB.getStatusCode()).isEqualTo(HttpStatus.OK);

    String accessA = (String) signupRespA.getBody().get("accessToken");
    String accessB = (String) signupRespB.getBody().get("accessToken");

    HttpHeaders headersA = new HttpHeaders();
    headersA.setBearerAuth(accessA);
    headersA.setContentType(MediaType.APPLICATION_JSON);

    HttpHeaders headersB = new HttpHeaders();
    headersB.setBearerAuth(accessB);
    headersB.setContentType(MediaType.APPLICATION_JSON);

    ResponseEntity<Map<String, Object>> groupResp = rest.exchange(
      "/api/v1/groups",
      HttpMethod.POST,
      new HttpEntity<>(Map.of("name", "Accept By Id"), headersA),
      MAP_TYPE
    );
    assertThat(groupResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    String groupId = (String) groupResp.getBody().get("id");

    ResponseEntity<Map<String, Object>> inviteResp = rest.exchange(
      "/api/v1/groups/" + groupId + "/invites",
      HttpMethod.POST,
      new HttpEntity<>(Map.of("email", emailB), headersA),
      MAP_TYPE
    );
    assertThat(inviteResp.getStatusCode()).isEqualTo(HttpStatus.OK);

    ResponseEntity<List<Map<String, Object>>> listPendingResp = rest.exchange(
      "/api/v1/invites?email=" + emailB.toLowerCase(),
      HttpMethod.GET,
      new HttpEntity<>(headersB),
      LIST_OF_MAP_TYPE
    );
    assertThat(listPendingResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(listPendingResp.getBody()).hasSize(1);
    String inviteId = (String) listPendingResp.getBody().get(0).get("id");

    ResponseEntity<Void> acceptResp = rest.exchange(
      "/api/v1/invites/" + inviteId + "/accept",
      HttpMethod.POST,
      new HttpEntity<>(headersB),
      Void.class
    );
    assertThat(acceptResp.getStatusCode()).isEqualTo(HttpStatus.OK);

    ResponseEntity<List<Map<String, Object>>> listGroupsResp = rest.exchange(
      "/api/v1/groups",
      HttpMethod.GET,
      new HttpEntity<>(headersB),
      LIST_OF_MAP_TYPE
    );
    assertThat(listGroupsResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(listGroupsResp.getBody()).extracting(g -> g.get("id")).contains(groupId);

    ResponseEntity<List<Map<String, Object>>> listPendingAfterResp = rest.exchange(
      "/api/v1/invites?email=" + emailB.toLowerCase(),
      HttpMethod.GET,
      new HttpEntity<>(headersB),
      LIST_OF_MAP_TYPE
    );
    assertThat(listPendingAfterResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(listPendingAfterResp.getBody()).isEmpty();
  }

  @Test
  void nonMemberCannotGetGroupDetails() {
    String emailA = "user_" + UUID.randomUUID() + "@example.com";
    String emailB = "user_" + UUID.randomUUID() + "@example.com";
    String password = "password123";

    ResponseEntity<Map<String, Object>> signupRespA = rest.exchange(
      "/api/v1/auth/signup",
      HttpMethod.POST,
      new HttpEntity<>(Map.of("email", emailA, "password", password, "displayName", "Member")),
      MAP_TYPE
    );
    ResponseEntity<Map<String, Object>> signupRespB = rest.exchange(
      "/api/v1/auth/signup",
      HttpMethod.POST,
      new HttpEntity<>(Map.of("email", emailB, "password", password, "displayName", "Outsider")),
      MAP_TYPE
    );
    assertThat(signupRespA.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(signupRespB.getStatusCode()).isEqualTo(HttpStatus.OK);

    String accessA = (String) signupRespA.getBody().get("accessToken");
    String accessB = (String) signupRespB.getBody().get("accessToken");

    HttpHeaders headersA = new HttpHeaders();
    headersA.setBearerAuth(accessA);
    headersA.setContentType(MediaType.APPLICATION_JSON);

    HttpHeaders headersB = new HttpHeaders();
    headersB.setBearerAuth(accessB);
    headersB.setContentType(MediaType.APPLICATION_JSON);

    ResponseEntity<Map<String, Object>> groupResp = rest.exchange(
      "/api/v1/groups",
      HttpMethod.POST,
      new HttpEntity<>(Map.of("name", "Private Group"), headersA),
      MAP_TYPE
    );
    assertThat(groupResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    String groupId = (String) groupResp.getBody().get("id");

    ResponseEntity<Map<String, Object>> detailsResp = rest.exchange(
      "/api/v1/groups/" + groupId + "/details",
      HttpMethod.GET,
      new HttpEntity<>(headersB),
      MAP_TYPE
    );
    assertThat(detailsResp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }
}
