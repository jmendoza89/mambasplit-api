package io.mambatech.mambasplit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GroupFlowIT {
  @Autowired private TestRestTemplate rest;

  @Test
  void groupInviteAcceptAndExpenseFlow() {
    String emailA = "user_" + UUID.randomUUID() + "@example.com";
    String emailB = "user_" + UUID.randomUUID() + "@example.com";
    String password = "password123";

    Map<String, Object> signupA = Map.of("email", emailA, "password", password, "displayName", "User A");
    Map<String, Object> signupB = Map.of("email", emailB, "password", password, "displayName", "User B");

    ResponseEntity<Map> signupRespA = rest.postForEntity("/api/v1/auth/signup", signupA, Map.class);
    ResponseEntity<Map> signupRespB = rest.postForEntity("/api/v1/auth/signup", signupB, Map.class);
    assertThat(signupRespA.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(signupRespB.getStatusCode()).isEqualTo(HttpStatus.OK);

    String accessA = (String) signupRespA.getBody().get("accessToken");
    String accessB = (String) signupRespB.getBody().get("accessToken");
    String userIdA = (String) ((Map<String, Object>) signupRespA.getBody().get("user")).get("id");
    String userIdB = (String) ((Map<String, Object>) signupRespB.getBody().get("user")).get("id");

    HttpHeaders headersA = new HttpHeaders();
    headersA.setBearerAuth(accessA);
    headersA.setContentType(MediaType.APPLICATION_JSON);

    HttpHeaders headersB = new HttpHeaders();
    headersB.setBearerAuth(accessB);
    headersB.setContentType(MediaType.APPLICATION_JSON);

    Map<String, Object> createGroup = Map.of("name", "Test Group");
    ResponseEntity<Map> groupResp = rest.exchange(
      "/api/v1/groups",
      HttpMethod.POST,
      new HttpEntity<>(createGroup, headersA),
      Map.class
    );
    assertThat(groupResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    String groupId = (String) groupResp.getBody().get("id");
    assertThat(groupId).isNotBlank();

    Map<String, Object> inviteBody = Map.of("email", emailB);
    ResponseEntity<Map> inviteResp = rest.exchange(
      "/api/v1/groups/" + groupId + "/invites",
      HttpMethod.POST,
      new HttpEntity<>(inviteBody, headersA),
      Map.class
    );
    assertThat(inviteResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    String token = (String) inviteResp.getBody().get("token");
    assertThat(token).isNotBlank();

    ResponseEntity<Void> acceptResp = rest.exchange(
      "/api/v1/invites/" + token + "/accept",
      HttpMethod.POST,
      new HttpEntity<>(headersB),
      Void.class
    );
    assertThat(acceptResp.getStatusCode()).isEqualTo(HttpStatus.OK);

    Map<String, Object> createEqual = Map.of(
      "description", "Dinner",
      "payerUserId", userIdA,
      "amountCents", 1000,
      "participants", List.of(userIdA, userIdB)
    );

    ResponseEntity<Map> expenseResp = rest.exchange(
      "/api/v1/groups/" + groupId + "/expenses/equal",
      HttpMethod.POST,
      new HttpEntity<>(createEqual, headersA),
      Map.class
    );
    assertThat(expenseResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat((String) expenseResp.getBody().get("expenseId")).isNotBlank();
  }
}