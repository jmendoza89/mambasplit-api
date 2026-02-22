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
      "/api/v1/invites/" + token + "/accept",
      HttpMethod.POST,
      new HttpEntity<>(headersC),
      MAP_TYPE
    );
    assertThat(wrongEmailAcceptResp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

    ResponseEntity<Void> acceptResp = rest.exchange(
      "/api/v1/invites/" + token + "/accept",
      HttpMethod.POST,
      new HttpEntity<>(headersB),
      Void.class
    );
    assertThat(acceptResp.getStatusCode()).isEqualTo(HttpStatus.OK);

    ResponseEntity<Map<String, Object>> reuseInviteResp = rest.exchange(
      "/api/v1/invites/" + token + "/accept",
      HttpMethod.POST,
      new HttpEntity<>(headersB),
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
      "/api/v1/invites/" + tokenForC + "/accept",
      HttpMethod.POST,
      new HttpEntity<>(headersC),
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
  }
}
