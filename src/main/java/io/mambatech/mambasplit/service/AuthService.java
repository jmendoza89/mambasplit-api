package io.mambatech.mambasplit.service;

import io.mambatech.mambasplit.domain.auth.RefreshToken;
import io.mambatech.mambasplit.domain.user.User;
import io.mambatech.mambasplit.repo.RefreshTokenRepository;
import io.mambatech.mambasplit.repo.UserRepository;
import io.mambatech.mambasplit.security.AppSecurityProperties;
import io.mambatech.mambasplit.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {
  private final UserRepository users;
  private final RefreshTokenRepository refreshTokens;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final AppSecurityProperties props;

  public AuthService(UserRepository users, RefreshTokenRepository refreshTokens, PasswordEncoder passwordEncoder,
                     JwtService jwtService, AppSecurityProperties props) {
    this.users = users;
    this.refreshTokens = refreshTokens;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
    this.props = props;
  }

  @Transactional
  public User signup(String email, String rawPassword, String displayName) {
    users.findByEmailIgnoreCase(email).ifPresent(u -> { throw new IllegalArgumentException("Email already in use"); });
    UUID id = UUID.randomUUID();
    String hash = passwordEncoder.encode(rawPassword);
    return users.save(new User(id, email.toLowerCase(), hash, displayName, Instant.now()));
  }

  public Optional<User> authenticate(String email, String rawPassword) {
    return users.findByEmailIgnoreCase(email).filter(u -> passwordEncoder.matches(rawPassword, u.getPasswordHash()));
  }

  @Transactional
  public Tokens issueTokens(User user) {
    String access = jwtService.createAccessToken(user.getId(), user.getEmail());
    String refresh = randomToken();
    String refreshHash = sha256Base64(refresh);
    Instant exp = Instant.now().plus(props.refreshTokenDays(), ChronoUnit.DAYS);
    refreshTokens.save(new RefreshToken(UUID.randomUUID(), user.getId(), refreshHash, exp, null, Instant.now()));
    return new Tokens(access, refresh);
  }

  @Transactional
  public Tokens refresh(String refreshTokenRaw) {
    String hash = sha256Base64(refreshTokenRaw);
    RefreshToken token = refreshTokens.findByTokenHash(hash).orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));
    if (token.getRevokedAt() != null) throw new IllegalArgumentException("Refresh token revoked");
    if (token.getExpiresAt().isBefore(Instant.now())) throw new IllegalArgumentException("Refresh token expired");
    token.revoke(Instant.now());
    refreshTokens.save(token);
    User user = users.findById(token.getUserId()).orElseThrow();
    return issueTokens(user);
  }

  @Transactional
  public void logout(String refreshTokenRaw) {
    String hash = sha256Base64(refreshTokenRaw);
    refreshTokens.findByTokenHash(hash).ifPresent(rt -> { rt.revoke(Instant.now()); refreshTokens.save(rt); });
  }

  public record Tokens(String accessToken, String refreshToken) {}

  private static String randomToken() {
    byte[] bytes = UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes) + "." +
      Base64.getUrlEncoder().withoutPadding().encodeToString(UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8));
  }

  private static String sha256Base64(String value) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      byte[] digest = md.digest(value.getBytes(StandardCharsets.UTF_8));
      return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
