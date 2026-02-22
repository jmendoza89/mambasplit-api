package io.mambatech.mambasplit.web;

import io.mambatech.mambasplit.security.AuthPrincipal;
import io.mambatech.mambasplit.service.GroupService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/invites")
public class InviteController {
  private final GroupService groups;

  public InviteController(GroupService groups) {
    this.groups = groups;
  }

  @PostMapping("/{token}/accept")
  public void accept(@AuthenticationPrincipal AuthPrincipal principal, @PathVariable String token) {
    groups.acceptInvite(token, principal.userId());
  }
}
