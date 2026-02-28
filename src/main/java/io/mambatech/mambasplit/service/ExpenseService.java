package io.mambatech.mambasplit.service;

import io.mambatech.mambasplit.domain.expense.Expense;
import io.mambatech.mambasplit.domain.expense.ExpenseSplit;
import io.mambatech.mambasplit.domain.money.Money;
import io.mambatech.mambasplit.exception.AuthorizationException;
import io.mambatech.mambasplit.exception.ResourceNotFoundException;
import io.mambatech.mambasplit.exception.ValidationException;
import io.mambatech.mambasplit.repo.ExpenseRepository;
import io.mambatech.mambasplit.repo.ExpenseSplitRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
public class ExpenseService {
  private static final Logger log = LoggerFactory.getLogger(ExpenseService.class);
  private final ExpenseRepository expenses;
  private final ExpenseSplitRepository splits;
  private final GroupService groupService;

  public ExpenseService(ExpenseRepository expenses, ExpenseSplitRepository splits, GroupService groupService) {
    this.expenses = expenses;
    this.splits = splits;
    this.groupService = groupService;
  }

  public record SplitExact(List<Item> items) {
    public record Item(UUID userId, long amountCents) {}
  }

  @Transactional
  public UUID createEqualSplitExpense(UUID groupId, UUID payerUserId, String description, long totalAmountCents, List<UUID> participants) {
    if (participants == null || participants.isEmpty()) {
      throw new ValidationException("Participants list cannot be empty");
    }
    if (totalAmountCents <= 0) {
      throw new ValidationException("Amount must be greater than 0");
    }
    if (new HashSet<>(participants).size() != participants.size()) {
      throw new ValidationException("Duplicate users in participants list");
    }

    // Validate all participants (including payer) are members of the group
    Set<UUID> memberIds = new HashSet<>(participants);
    memberIds.add(payerUserId);
    groupService.requireMembers(groupId, memberIds);

    UUID expenseId = UUID.randomUUID();
    expenses.save(new Expense(expenseId, groupId, payerUserId, description, Money.ofCents(totalAmountCents), Instant.now()));

    List<UUID> sorted = new ArrayList<>(participants);
    sorted.sort(Comparator.comparing(UUID::toString));

    long base = totalAmountCents / sorted.size();
    long rem = totalAmountCents % sorted.size();

    for (int i = 0; i < sorted.size(); i++) {
      long owed = base + (i < rem ? 1 : 0);
      splits.save(new ExpenseSplit(UUID.randomUUID(), expenseId, sorted.get(i), Money.ofCents(owed)));
    }

    log.info("Equal split expense created: expenseId={}, groupId={}, amount={}", expenseId, groupId, totalAmountCents);
    return expenseId;
  }

  @Transactional
  public UUID createExactSplitExpense(UUID groupId, UUID payerUserId, String description, long totalAmountCents, List<SplitExact.Item> items) {
    if (items == null || items.isEmpty()) {
      throw new ValidationException("Split items list cannot be empty");
    }
    long sum = items.stream().mapToLong(SplitExact.Item::amountCents).sum();
    if (sum != totalAmountCents) {
      throw new ValidationException("Split sum (" + sum + ") must equal total amount (" + totalAmountCents + ")");
    }

    // Validate all participants (including payer) are members of the group
    Set<UUID> memberIds = new HashSet<>();
    memberIds.add(payerUserId);
    for (SplitExact.Item item : items) {
      memberIds.add(item.userId());
    }
    groupService.requireMembers(groupId, memberIds);

    UUID expenseId = UUID.randomUUID();
    expenses.save(new Expense(expenseId, groupId, payerUserId, description, Money.ofCents(totalAmountCents), Instant.now()));

    Set<UUID> seen = new HashSet<>();
    for (SplitExact.Item it : items) {
      if (!seen.add(it.userId())) {
        throw new ValidationException("Duplicate user in split items");
      }
      if (it.amountCents() < 0) {
        throw new ValidationException("Split amount cannot be negative");
      }
      splits.save(new ExpenseSplit(UUID.randomUUID(), expenseId, it.userId(), Money.ofCents(it.amountCents())));
    }

    log.info("Exact split expense created: expenseId={}, groupId={}, amount={}", expenseId, groupId, totalAmountCents);
    return expenseId;
  }

  @Transactional
  public void deleteExpense(UUID groupId, UUID expenseId, UUID actorUserId) {
    Expense expense = expenses.findByIdAndGroupId(expenseId, groupId)
      .orElseThrow(() -> new ResourceNotFoundException("Expense", expenseId.toString()));
    if (!expense.getPayerUserId().equals(actorUserId)) {
      throw new AuthorizationException("delete", "expense " + expenseId);
    }
    splits.deleteByExpenseId(expenseId);
    expenses.delete(expense);
  }
}
