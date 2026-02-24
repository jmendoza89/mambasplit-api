package io.mambatech.mambasplit.web;

import io.mambatech.mambasplit.domain.user.User;
import io.mambatech.mambasplit.service.AuthService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
  private final AuthService auth;

  public AuthController(AuthService auth) {
    this.auth = auth;
  }

  public record SignupRequest(@Email @NotBlank String email, @NotBlank @Size(min=8,max=200) String password,
                              @NotBlank @Size(max=120) String displayName) {}
  public record LoginRequest(@Email @NotBlank String email, @NotBlank String password) {}
  public record GoogleAuthRequest(@NotBlank String idToken) {}
  public record RefreshRequest(@NotBlank String refreshToken) {}
  public record LogoutRequest(@NotBlank String refreshToken) {}

  public record AuthResponse(String accessToken, String refreshToken, UserDto user) {}
  public record UserDto(String id, String email, String displayName) {
    static UserDto from(User u) { return new UserDto(u.getId().toString(), u.getEmail(), u.getDisplayName()); }
  }

  @PostMapping("/signup")
  public ResponseEntity<AuthResponse> signup(@Valid @RequestBody SignupRequest req) {
    User u = auth.signup(req.email(), req.password(), req.displayName());
    var tokens = auth.issueTokens(u);
    return ResponseEntity.ok(new AuthResponse(tokens.accessToken(), tokens.refreshToken(), UserDto.from(u)));
  }

  @PostMapping("/login")
  public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req) {
    User u = auth.authenticate(req.email(), req.password()).orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));
    var tokens = auth.issueTokens(u);
    return ResponseEntity.ok(new AuthResponse(tokens.accessToken(), tokens.refreshToken(), UserDto.from(u)));
  }

  @PostMapping("/google")
  public ResponseEntity<AuthResponse> google(@Valid @RequestBody GoogleAuthRequest req) {
    User u = auth.authenticateGoogle(req.idToken());
    var tokens = auth.issueTokens(u);
    return ResponseEntity.ok(new AuthResponse(tokens.accessToken(), tokens.refreshToken(), UserDto.from(u)));
  }

  @PostMapping("/refresh")
  public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest req) {
    var tokens = auth.refresh(req.refreshToken());
    return ResponseEntity.ok(new AuthResponse(tokens.accessToken(), tokens.refreshToken(), null));
  }

  @PostMapping("/logout")
  public ResponseEntity<Void> logout(@Valid @RequestBody LogoutRequest req) {
    auth.logout(req.refreshToken());
    return ResponseEntity.noContent().build();
  }
}
