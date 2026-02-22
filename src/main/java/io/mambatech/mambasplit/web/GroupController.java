package io.mambatech.mambasplit.web;

import io.mambatech.mambasplit.domain.group.Group;
import io.mambatech.mambasplit.domain.invite.Invite;
import io.mambatech.mambasplit.security.AuthPrincipal;
import io.mambatech.mambasplit.service.GroupService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/groups")
public class GroupController {
  private final GroupService groups;

  public GroupController(GroupService groups) {
    this.groups = groups;
  }

  public record CreateGroupRequest(@NotBlank @Size(max=200) String name) {}
  public record GroupDto(String id, String name) {
    static GroupDto from(Group g) { return new GroupDto(g.getId().toString(), g.getName()); }
  }

  @PostMapping
  public GroupDto create(@AuthenticationPrincipal AuthPrincipal principal, @Valid @RequestBody CreateGroupRequest req) {
    return GroupDto.from(groups.createGroup(principal.userId(), req.name()));
  }

  @GetMapping
  public List<GroupDto> list(@AuthenticationPrincipal AuthPrincipal principal) {
    return groups.listGroupsForUser(principal.userId()).stream().map(GroupDto::from).toList();
  }

  public record InviteRequest(@NotBlank String email) {}
  public record InviteDto(String token, String email, String expiresAt) {
    static InviteDto from(Invite i) { return new InviteDto(i.getToken(), i.getEmail(), i.getExpiresAt().toString()); }
  }

  @PostMapping("/{groupId}/invites")
  public InviteDto invite(@AuthenticationPrincipal AuthPrincipal principal, @PathVariable String groupId, @Valid @RequestBody InviteRequest req) {
    UUID gid = UUID.fromString(groupId);
    groups.requireMember(gid, principal.userId());
    return InviteDto.from(groups.createInvite(gid, req.email()));
  }
}
