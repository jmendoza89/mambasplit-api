package io.mambatech.mambasplit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthFlowIT {
  @Autowired private TestRestTemplate rest;

  @Test
  void signupLoginRefreshLogoutFlow() {
    String email = "user_" + UUID.randomUUID() + "@example.com";
    String password = "password123";

    Map<String, Object> signupBody = Map.of(
      "email", email,
      "password", password,
      "displayName", "User A"
    );

    ResponseEntity<Map> signupResp = rest.postForEntity("/api/v1/auth/signup", signupBody, Map.class);
    assertThat(signupResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    Map<String, Object> signupPayload = signupResp.getBody();
    assertThat(signupPayload).isNotNull();
    String refreshToken1 = (String) signupPayload.get("refreshToken");
    String accessToken1 = (String) signupPayload.get("accessToken");
    assertThat(refreshToken1).isNotBlank();
    assertThat(accessToken1).isNotBlank();

    Map<String, Object> loginBody = Map.of("email", email, "password", password);
    ResponseEntity<Map> loginResp = rest.postForEntity("/api/v1/auth/login", loginBody, Map.class);
    assertThat(loginResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    Map<String, Object> loginPayload = loginResp.getBody();
    assertThat(loginPayload).isNotNull();
    assertThat((String) loginPayload.get("accessToken")).isNotBlank();
    assertThat((String) loginPayload.get("refreshToken")).isNotBlank();

    Map<String, Object> refreshBody1 = Map.of("refreshToken", refreshToken1);
    ResponseEntity<Map> refreshResp = rest.postForEntity("/api/v1/auth/refresh", refreshBody1, Map.class);
    assertThat(refreshResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    Map<String, Object> refreshPayload = refreshResp.getBody();
    assertThat(refreshPayload).isNotNull();
    String refreshToken2 = (String) refreshPayload.get("refreshToken");
    assertThat(refreshToken2).isNotBlank();
    assertThat(refreshToken2).isNotEqualTo(refreshToken1);

    ResponseEntity<Map> refreshOld = rest.postForEntity("/api/v1/auth/refresh", refreshBody1, Map.class);
    assertThat(refreshOld.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

    Map<String, Object> logoutBody = Map.of("refreshToken", refreshToken2);
    ResponseEntity<Void> logoutResp = rest.postForEntity("/api/v1/auth/logout", logoutBody, Void.class);
    assertThat(logoutResp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

    ResponseEntity<Map> refreshAfterLogout = rest.postForEntity("/api/v1/auth/refresh", logoutBody, Map.class);
    assertThat(refreshAfterLogout.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }
}