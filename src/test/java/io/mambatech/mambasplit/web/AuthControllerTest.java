package io.mambatech.mambasplit.web;

import io.mambatech.mambasplit.domain.user.User;
import io.mambatech.mambasplit.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest {
  private final AuthService authService = mock(AuthService.class);
  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
    validator.afterPropertiesSet();
    mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(authService))
      .setControllerAdvice(new ApiExceptionHandler())
      .setValidator(validator)
      .build();
  }

  @Test
  void googleAuth_success() throws Exception {
    User user = new User(UUID.randomUUID(), "google@example.com", "hash", "Google User", Instant.now(), "sub-1");
    when(authService.authenticateGoogle("id-token")).thenReturn(user);
    when(authService.issueTokens(user)).thenReturn(new AuthService.Tokens("access-token", "refresh-token"));

    mockMvc.perform(post("/api/v1/auth/google")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"idToken\":\"id-token\"}"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.accessToken").value("access-token"))
      .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
      .andExpect(jsonPath("$.user.id").value(user.getId().toString()))
      .andExpect(jsonPath("$.user.email").value("google@example.com"))
      .andExpect(jsonPath("$.user.displayName").value("Google User"));
  }

  @Test
  void googleAuth_invalidInput() throws Exception {
    mockMvc.perform(post("/api/v1/auth/google")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{}"))
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
  }
}
