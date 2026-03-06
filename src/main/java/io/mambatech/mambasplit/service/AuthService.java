package io.mambatech.mambasplit.service;

import io.mambatech.mambasplit.domain.auth.RefreshToken;
import io.mambatech.mambasplit.domain.user.User;
import io.mambatech.mambasplit.exception.AuthenticationException;
import io.mambatech.mambasplit.exception.ConflictException;
import io.mambatech.mambasplit.exception.ResourceNotFoundException;
import io.mambatech.mambasplit.exception.ValidationException;
import io.mambatech.mambasplit.repo.RefreshTokenRepository;
import io.mambatech.mambasplit.repo.UserRepository;
import io.mambatech.mambasplit.security.AppSecurityProperties;
import io.mambatech.mambasplit.security.JwtService;
import io.mambatech.mambasplit.security.TokenCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {
  private static final Logger log = LoggerFactory.getLogger(AuthService.class);
  private final UserRepository users;
  private final RefreshTokenRepository refreshTokens;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final AppSecurityProperties props;
  private final GoogleTokenVerifier googleTokenVerifier;

  public AuthService(UserRepository users, RefreshTokenRepository refreshTokens, PasswordEncoder passwordEncoder,
                     JwtService jwtService, AppSecurityProperties props, GoogleTokenVerifier googleTokenVerifier) {
    this.users = users;
    this.refreshTokens = refreshTokens;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
    this.props = props;
    this.googleTokenVerifier = googleTokenVerifier;
  }

  @Transactional
  public User signup(String email, String rawPassword, String displayName) {
    users.findByEmailIgnoreCase(email).ifPresent(u -> { 
      log.warn("Signup attempt with email that already exists: {}", email);
      throw new ConflictException("Email already in use: " + email); 
    });
    UUID id = UUID.randomUUID();
    String hash = passwordEncoder.encode(rawPassword);
    User user = users.save(new User(id, email.toLowerCase(), hash, displayName, Instant.now()));
    log.info("User signed up successfully: userId={}, email={}", user.getId(), email);
    return user;
  }

  public Optional<User> authenticate(String email, String rawPassword) {
    Optional<User> userOpt = users.findByEmailIgnoreCase(email)
      .filter(u -> passwordEncoder.matches(rawPassword, u.getPasswordHash()));
    if (userOpt.isEmpty()) {
      log.warn("Failed authentication attempt for email: {}", email);
    } else {
      log.info("Successful authentication: userId={}, email={}", userOpt.get().getId(), email);
    }
    return userOpt;
  }

  @Transactional
  public User authenticateGoogle(String idToken) {
    GoogleTokenVerifier.GoogleUser googleUser = googleTokenVerifier.verify(idToken);
    if (!googleUser.emailVerified()) {
      log.warn("Google authentication failed: email not verified for sub={}", googleUser.sub());
      throw new AuthenticationException("Google email is not verified");
    }

    return users.findByGoogleSub(googleUser.sub()).map(user -> {
      log.info("Google authentication successful via sub: userId={}, email={}", user.getId(), user.getEmail());
      return updateFromGoogle(user, googleUser);
    }).orElseGet(() -> {
      User byEmail = users.findByEmailIgnoreCase(googleUser.email())
        .orElseGet(() -> createGoogleUser(googleUser));

      if (byEmail.getGoogleSub() == null) {
        byEmail.setGoogleSub(googleUser.sub());
        log.info("Linked existing user to Google account: userId={}, email={}", byEmail.getId(), byEmail.getEmail());
        return updateFromGoogle(byEmail, googleUser);
      }

      if (!googleUser.sub().equals(byEmail.getGoogleSub())) {
        log.warn("Attempted Google login with email linked to different account: email={}", googleUser.email());
        throw new ConflictException("Email already linked to a different Google account");
      }

      log.info("Google authentication successful via email: userId={}, email={}", byEmail.getId(), byEmail.getEmail());
      return updateFromGoogle(byEmail, googleUser);
    });
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
    
    // Atomic revocation with expiry check to prevent race conditions
    // This UPDATE query checks token validity and revokes in a single atomic operation
    int revoked = refreshTokens.revokeIfActive(hash, now, now);
    if (revoked == 0) {
      log.warn("Refresh token refresh attempt with invalid or expired token");
      throw new AuthenticationException("Invalid or expired refresh token");
    }
    
    RefreshToken token = refreshTokens.findByTokenHash(hash)
      .orElseThrow(() -> new ResourceNotFoundException("RefreshToken", hash));
    User user = users.findById(token.getUserId())
      .orElseThrow(() -> new ResourceNotFoundException("User", token.getUserId().toString()));
    log.info("Refresh token refreshed successfully: userId={}", user.getId());
    return issueTokens(user);
  }

  @Transactional
  public void logout(String refreshTokenRaw) {
    String hash = TokenCodec.sha256Base64Url(refreshTokenRaw);
    refreshTokens.findByTokenHash(hash).ifPresent(rt -> { 
      rt.revoke(Instant.now()); 
      refreshTokens.save(rt);
      log.info("User logged out: userId={}", rt.getUserId());
    });
  }

  private User updateFromGoogle(User user, GoogleTokenVerifier.GoogleUser googleUser) {
    // Keep existing profile unless it's effectively empty.
    if (user.getDisplayName() == null || user.getDisplayName().isBlank()) {
      String fallback = (googleUser.name() != null && !googleUser.name().isBlank()) ? googleUser.name() : user.getEmail();
      user.setDisplayName(fallback);
    }
    return user;
  }

  private User createGoogleUser(GoogleTokenVerifier.GoogleUser googleUser) {
    UUID id = UUID.randomUUID();
    String displayName = (googleUser.name() != null && !googleUser.name().isBlank()) ? googleUser.name() : googleUser.email();
    String passwordHash = passwordEncoder.encode(TokenCodec.randomUrlToken(48));
    return users.save(new User(id, googleUser.email().toLowerCase(), passwordHash, displayName, Instant.now(), googleUser.sub()));
  }

  public record Tokens(String accessToken, String refreshToken) {}
}
