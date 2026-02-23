package io.mambatech.mambasplit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class GroupFlowIT extends ITBase {
  @Autowired private TestRestTemplate rest;
  private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
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
