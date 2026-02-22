package io.mambatech.mambasplit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AuthFlowIT extends ITBase {
  @Autowired private TestRestTemplate rest;
  private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
    new ParameterizedTypeReference<>() {};

  @Test
  void signupLoginRefreshLogoutFlow() {
    String email = "user_" + UUID.randomUUID() + "@example.com";
    String password = "password123";

    Map<String, Object> signupBody = Map.of(
      "email", email,
      "password", password,
      "displayName", "User A"
    );

    ResponseEntity<Map<String, Object>> signupResp = rest.exchange(
      "/api/v1/auth/signup",
      HttpMethod.POST,
      new HttpEntity<>(signupBody),
      MAP_TYPE
    );
    assertThat(signupResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    Map<String, Object> signupPayload = signupResp.getBody();
    assertThat(signupPayload).isNotNull();
    String refreshToken1 = (String) signupPayload.get("refreshToken");
    String accessToken1 = (String) signupPayload.get("accessToken");
    assertThat(refreshToken1).isNotBlank();
    assertThat(accessToken1).isNotBlank();

    Map<String, Object> loginBody = Map.of("email", email, "password", password);
    ResponseEntity<Map<String, Object>> loginResp = rest.exchange(
      "/api/v1/auth/login",
      HttpMethod.POST,
      new HttpEntity<>(loginBody),
      MAP_TYPE
    );
    assertThat(loginResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    Map<String, Object> loginPayload = loginResp.getBody();
    assertThat(loginPayload).isNotNull();
    assertThat((String) loginPayload.get("accessToken")).isNotBlank();
    assertThat((String) loginPayload.get("refreshToken")).isNotBlank();

    Map<String, Object> refreshBody1 = Map.of("refreshToken", refreshToken1);
    ResponseEntity<Map<String, Object>> refreshResp = rest.exchange(
      "/api/v1/auth/refresh",
      HttpMethod.POST,
      new HttpEntity<>(refreshBody1),
      MAP_TYPE
    );
    assertThat(refreshResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    Map<String, Object> refreshPayload = refreshResp.getBody();
    assertThat(refreshPayload).isNotNull();
    String refreshToken2 = (String) refreshPayload.get("refreshToken");
    assertThat(refreshToken2).isNotBlank();
    assertThat(refreshToken2).isNotEqualTo(refreshToken1);

    ResponseEntity<Map<String, Object>> refreshOld = rest.exchange(
      "/api/v1/auth/refresh",
      HttpMethod.POST,
      new HttpEntity<>(refreshBody1),
      MAP_TYPE
    );
    assertThat(refreshOld.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

    Map<String, Object> logoutBody = Map.of("refreshToken", refreshToken2);
    ResponseEntity<Void> logoutResp = rest.postForEntity("/api/v1/auth/logout", logoutBody, Void.class);
    assertThat(logoutResp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

    ResponseEntity<Map<String, Object>> refreshAfterLogout = rest.exchange(
      "/api/v1/auth/refresh",
      HttpMethod.POST,
      new HttpEntity<>(logoutBody),
      MAP_TYPE
    );
    assertThat(refreshAfterLogout.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }
}
