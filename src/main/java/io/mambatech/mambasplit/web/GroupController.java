package io.mambatech.mambasplit.web;

import io.mambatech.mambasplit.domain.group.Group;
import io.mambatech.mambasplit.security.AuthPrincipal;
import io.mambatech.mambasplit.service.GroupService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
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

  public record GroupDetailsDto(
    GroupInfoDto group,
    MeInfoDto me,
    List<MemberInfoDto> members,
    List<ExpenseInfoDto> expenses,
    SummaryDto summary
  ) {
    static GroupDetailsDto from(GroupService.GroupDetails details) {
      return new GroupDetailsDto(
        GroupInfoDto.from(details.group()),
        MeInfoDto.from(details.me()),
        details.members().stream().map(MemberInfoDto::from).toList(),
        details.expenses().stream().map(ExpenseInfoDto::from).toList(),
        SummaryDto.from(details.summary())
      );
    }
  }

  public record GroupInfoDto(String id, String name, String createdBy, String createdAt) {
    static GroupInfoDto from(GroupService.GroupInfo g) {
      return new GroupInfoDto(g.id().toString(), g.name(), g.createdBy().toString(), g.createdAt().toString());
    }
  }

  public record MeInfoDto(String userId, String role, long netBalanceCents) {
    static MeInfoDto from(GroupService.MeInfo m) {
      return new MeInfoDto(m.userId().toString(), m.role(), m.netBalanceCents());
    }
  }

  public record MemberInfoDto(
    String userId,
    String displayName,
    String email,
    String role,
    String joinedAt,
    long netBalanceCents
  ) {
    static MemberInfoDto from(GroupService.MemberInfo m) {
      return new MemberInfoDto(
        m.userId().toString(),
        m.displayName(),
        m.email(),
        m.role(),
        m.joinedAt().toString(),
        m.netBalanceCents()
      );
    }
  }

  public record ExpenseInfoDto(
    String id,
    String description,
    long amountCents,
    String payerUserId,
    String createdAt,
    List<ExpenseSplitInfoDto> splits
  ) {
    static ExpenseInfoDto from(GroupService.ExpenseInfo e) {
      return new ExpenseInfoDto(
        e.id().toString(),
        e.description(),
        e.amountCents(),
        e.payerUserId().toString(),
        e.createdAt().toString(),
        e.splits().stream().map(ExpenseSplitInfoDto::from).toList()
      );
    }
  }

  public record ExpenseSplitInfoDto(String userId, long amountOwedCents) {
    static ExpenseSplitInfoDto from(GroupService.ExpenseSplitInfo s) {
      return new ExpenseSplitInfoDto(s.userId().toString(), s.amountOwedCents());
    }
  }

  public record SummaryDto(int expenseCount, long totalExpenseAmountCents) {
    static SummaryDto from(GroupService.Summary s) {
      return new SummaryDto(s.expenseCount(), s.totalExpenseAmountCents());
    }
  }

  @GetMapping("/{groupId}/details")
  public GroupDetailsDto details(@AuthenticationPrincipal AuthPrincipal principal, @PathVariable String groupId) {
    return GroupDetailsDto.from(groups.getGroupDetails(UUID.fromString(groupId), principal.userId()));
  }

  @DeleteMapping("/{groupId}")
  public ResponseEntity<Void> delete(@AuthenticationPrincipal AuthPrincipal principal, @PathVariable String groupId) {
    groups.deleteGroup(UUID.fromString(groupId), principal.userId());
    return ResponseEntity.noContent().build();
  }

  public record InviteRequest(@NotBlank @Email @Size(max=320) String email) {}
  public record InviteDto(String token, String email, String expiresAt) {
    static InviteDto from(GroupService.CreatedInvite i) { return new InviteDto(i.token(), i.email(), i.expiresAt().toString()); }
  }

  @PostMapping("/{groupId}/invites")
  public InviteDto invite(@AuthenticationPrincipal AuthPrincipal principal, @PathVariable String groupId, @Valid @RequestBody InviteRequest req) {
    UUID gid = UUID.fromString(groupId);
    groups.requireMember(gid, principal.userId());
    return InviteDto.from(groups.createInvite(gid, req.email()));
  }
}
