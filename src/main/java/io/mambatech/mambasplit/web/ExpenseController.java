package io.mambatech.mambasplit.web;

import io.mambatech.mambasplit.security.AuthPrincipal;
import io.mambatech.mambasplit.service.ExpenseService;
import io.mambatech.mambasplit.service.GroupService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/groups/{groupId}/expenses")
public class ExpenseController {
  private final GroupService groups;
  private final ExpenseService expenseService;

  public ExpenseController(GroupService groups, ExpenseService expenseService) {
    this.groups = groups; this.expenseService = expenseService;
  }

  public record CreateEqualExpenseRequest(@NotBlank String description, @NotNull String payerUserId,
                                         @Positive long amountCents, @NotNull List<String> participants) {}

  public record CreateExactExpenseRequest(@NotBlank String description, @NotNull String payerUserId,
                                         @Positive long amountCents, @NotNull List<Item> items) {
    public record Item(@NotNull String userId, long amountCents) {}
  }

  public record CreateExpenseResponse(String expenseId) {}

  @PostMapping("/equal")
  public CreateExpenseResponse createEqual(@AuthenticationPrincipal AuthPrincipal principal, @PathVariable String groupId,
                                          @Valid @RequestBody CreateEqualExpenseRequest req) {
    UUID gid = UUID.fromString(groupId);
    groups.requireMember(gid, principal.userId());
    UUID payer = UUID.fromString(req.payerUserId());
    List<UUID> participants = req.participants().stream().map(UUID::fromString).toList();
    UUID id = expenseService.createEqualSplitExpense(gid, payer, req.description(), req.amountCents(), participants);
    return new CreateExpenseResponse(id.toString());
  }

  @PostMapping("/exact")
  public CreateExpenseResponse createExact(@AuthenticationPrincipal AuthPrincipal principal, @PathVariable String groupId,
                                          @Valid @RequestBody CreateExactExpenseRequest req) {
    UUID gid = UUID.fromString(groupId);
    groups.requireMember(gid, principal.userId());
    UUID payer = UUID.fromString(req.payerUserId());
    var items = req.items().stream().map(i -> new ExpenseService.SplitExact.Item(UUID.fromString(i.userId()), i.amountCents())).toList();
    UUID id = expenseService.createExactSplitExpense(gid, payer, req.description(), req.amountCents(), items);
    return new CreateExpenseResponse(id.toString());
  }

  @DeleteMapping("/{expenseId}")
  public ResponseEntity<Void> delete(@AuthenticationPrincipal AuthPrincipal principal, @PathVariable String groupId, @PathVariable String expenseId) {
    UUID gid = UUID.fromString(groupId);
    groups.requireMember(gid, principal.userId());
    expenseService.deleteExpense(gid, UUID.fromString(expenseId), principal.userId());
    return ResponseEntity.noContent().build();
  }
}
