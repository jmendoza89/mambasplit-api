package io.mambatech.mambasplit.web;

import io.mambatech.mambasplit.security.AuthPrincipal;
import io.mambatech.mambasplit.service.GroupService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/invites")
@Validated
public class InviteController {
  private final GroupService groups;

  public InviteController(GroupService groups) {
    this.groups = groups;
  }

  public record AcceptInviteRequest(@NotBlank String token) {}
  public record PendingInviteDto(
    String id,
    String groupId,
    String groupName,
    String email,
    String expiresAt,
    String createdAt
  ) {
    static PendingInviteDto from(GroupService.PendingInvite invite) {
      return new PendingInviteDto(
        invite.id().toString(),
        invite.groupId().toString(),
        invite.groupName(),
        invite.email(),
        invite.expiresAt().toString(),
        invite.createdAt().toString()
      );
    }
  }

  @GetMapping
  public List<PendingInviteDto> listPending(
    @AuthenticationPrincipal AuthPrincipal principal,
    @RequestParam @NotBlank @Email @Size(max=320) String email
  ) {
    return groups.listPendingInvitesForEmail(email, principal.userId()).stream()
      .map(PendingInviteDto::from)
      .toList();
  }

  @PostMapping("/accept")
  public void accept(@AuthenticationPrincipal AuthPrincipal principal, @Valid @RequestBody AcceptInviteRequest req) {
    groups.acceptInvite(req.token(), principal.userId());
  }

  @PostMapping("/{inviteId}/accept")
  public void acceptById(@AuthenticationPrincipal AuthPrincipal principal, @PathVariable String inviteId) {
    groups.acceptInviteById(UUID.fromString(inviteId), principal.userId());
  }
}
