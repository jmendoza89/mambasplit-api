package io.mambatech.mambasplit.service;

import io.mambatech.mambasplit.domain.user.User;
import io.mambatech.mambasplit.repo.RefreshTokenRepository;
import io.mambatech.mambasplit.repo.UserRepository;
import io.mambatech.mambasplit.security.AppSecurityProperties;
import io.mambatech.mambasplit.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
  @Mock private UserRepository users;
  @Mock private RefreshTokenRepository refreshTokens;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private JwtService jwtService;
  @Mock private GoogleTokenVerifier googleTokenVerifier;

  private AuthService authService;

  @BeforeEach
  void setUp() {
    authService = new AuthService(
      users,
      refreshTokens,
      passwordEncoder,
      jwtService,
      new AppSecurityProperties("issuer", "secretsecretsecretsecret", 15, 30),
      googleTokenVerifier
    );
  }

  @Test
  void authenticateGoogle_existingByGoogleSub() {
    GoogleTokenVerifier.GoogleUser googleUser =
      new GoogleTokenVerifier.GoogleUser("sub-1", "user@example.com", "User Name", "https://img", true);
    User existing = new User(UUID.randomUUID(), "user@example.com", "hash", "Existing", Instant.now(), "sub-1");

    when(googleTokenVerifier.verify("id-token")).thenReturn(googleUser);
    when(users.findByGoogleSub("sub-1")).thenReturn(Optional.of(existing));

    User result = authService.authenticateGoogle("id-token");

    assertThat(result).isSameAs(existing);
    verify(users, never()).findByEmailIgnoreCase(any());
    verify(users, never()).save(any());
  }

  @Test
  void authenticateGoogle_linkByEmailWhenGoogleSubMissing() {
    GoogleTokenVerifier.GoogleUser googleUser =
      new GoogleTokenVerifier.GoogleUser("sub-2", "user@example.com", "User Name", "https://img", true);
    User existing = new User(UUID.randomUUID(), "user@example.com", "hash", "Existing", Instant.now(), null);

    when(googleTokenVerifier.verify("id-token")).thenReturn(googleUser);
    when(users.findByGoogleSub("sub-2")).thenReturn(Optional.empty());
    when(users.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(existing));

    User result = authService.authenticateGoogle("id-token");

    assertThat(result).isSameAs(existing);
    assertThat(existing.getGoogleSub()).isEqualTo("sub-2");
    verify(users, never()).save(any());
  }

  @Test
  void authenticateGoogle_createsNewUserWhenNoMatches() {
    GoogleTokenVerifier.GoogleUser googleUser =
      new GoogleTokenVerifier.GoogleUser("sub-3", "new@example.com", "New User", "https://img", true);

    when(googleTokenVerifier.verify("id-token")).thenReturn(googleUser);
    when(users.findByGoogleSub("sub-3")).thenReturn(Optional.empty());
    when(users.findByEmailIgnoreCase("new@example.com")).thenReturn(Optional.empty());
    when(passwordEncoder.encode(any())).thenReturn("encoded");
    when(users.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

    User result = authService.authenticateGoogle("id-token");

    assertThat(result.getEmail()).isEqualTo("new@example.com");
    assertThat(result.getGoogleSub()).isEqualTo("sub-3");
    assertThat(result.getDisplayName()).isEqualTo("New User");
    assertThat(result.getPasswordHash()).isEqualTo("encoded");
    ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
    verify(users).save(captor.capture());
    assertThat(captor.getValue().getGoogleSub()).isEqualTo("sub-3");
  }

  @Test
  void authenticateGoogle_rejectsConflictingGoogleSubForEmail() {
    GoogleTokenVerifier.GoogleUser googleUser =
      new GoogleTokenVerifier.GoogleUser("sub-4", "user@example.com", "User Name", "https://img", true);
    User existing = new User(UUID.randomUUID(), "user@example.com", "hash", "Existing", Instant.now(), "other-sub");

    when(googleTokenVerifier.verify("id-token")).thenReturn(googleUser);
    when(users.findByGoogleSub("sub-4")).thenReturn(Optional.empty());
    when(users.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(existing));

    assertThatThrownBy(() -> authService.authenticateGoogle("id-token"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Email already linked to a different Google account");
  }

  @Test
  void authenticateGoogle_rejectsUnverifiedEmail() {
    GoogleTokenVerifier.GoogleUser googleUser =
      new GoogleTokenVerifier.GoogleUser("sub-5", "user@example.com", "User Name", "https://img", false);
    when(googleTokenVerifier.verify("id-token")).thenReturn(googleUser);

    assertThatThrownBy(() -> authService.authenticateGoogle("id-token"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Google email is not verified");
  }
}
