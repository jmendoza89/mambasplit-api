package io.mambatech.mambasplit.service;

import io.mambatech.mambasplit.domain.auth.RefreshToken;
import io.mambatech.mambasplit.domain.user.User;
import io.mambatech.mambasplit.repo.RefreshTokenRepository;
import io.mambatech.mambasplit.repo.UserRepository;
import io.mambatech.mambasplit.security.AppSecurityProperties;
import io.mambatech.mambasplit.security.JwtService;
import io.mambatech.mambasplit.security.TokenCodec;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
    String refresh = TokenCodec.randomUrlToken(48);
    String refreshHash = TokenCodec.sha256Base64Url(refresh);
    Instant exp = Instant.now().plus(props.refreshTokenDays(), ChronoUnit.DAYS);
    refreshTokens.save(new RefreshToken(UUID.randomUUID(), user.getId(), refreshHash, exp, null, Instant.now()));
    return new Tokens(access, refresh);
  }

  @Transactional
  public Tokens refresh(String refreshTokenRaw) {
    String hash = TokenCodec.sha256Base64Url(refreshTokenRaw);
    Instant now = Instant.now();
    int revoked = refreshTokens.revokeIfActive(hash, now, now);
    if (revoked == 0) throw new IllegalArgumentException("Invalid or expired refresh token");
    RefreshToken token = refreshTokens.findByTokenHash(hash)
      .orElseThrow(() -> new IllegalStateException("Refresh token not found after revoke"));
    User user = users.findById(token.getUserId()).orElseThrow();
    return issueTokens(user);
  }

  @Transactional
  public void logout(String refreshTokenRaw) {
    String hash = TokenCodec.sha256Base64Url(refreshTokenRaw);
    refreshTokens.findByTokenHash(hash).ifPresent(rt -> { rt.revoke(Instant.now()); refreshTokens.save(rt); });
  }

  public record Tokens(String accessToken, String refreshToken) {}
}
