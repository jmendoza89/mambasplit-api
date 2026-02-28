package io.mambatech.mambasplit.web;

import io.mambatech.mambasplit.exception.ResourceNotFoundException;
import io.mambatech.mambasplit.repo.UserRepository;
import io.mambatech.mambasplit.security.AuthPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/me")
public class MeController {
  private final UserRepository users;

  public MeController(UserRepository users) {
    this.users = users;
  }

  public record MeResponse(String id, String email, String displayName) {}
  public record UpdateMeRequest(@NotBlank @Size(max=120) String displayName) {}

  @GetMapping
  public MeResponse me(@AuthenticationPrincipal AuthPrincipal principal) {
    var u = users.findById(principal.userId())
      .orElseThrow(() -> new ResourceNotFoundException("User", principal.userId().toString()));
    return new MeResponse(u.getId().toString(), u.getEmail(), u.getDisplayName());
  }

  @PatchMapping
  public MeResponse update(@AuthenticationPrincipal AuthPrincipal principal, @Valid @RequestBody UpdateMeRequest req) {
    var u = users.findById(principal.userId())
      .orElseThrow(() -> new ResourceNotFoundException("User", principal.userId().toString()));
    u.setDisplayName(req.displayName());
    users.save(u);
    return new MeResponse(u.getId().toString(), u.getEmail(), u.getDisplayName());
  }
}
