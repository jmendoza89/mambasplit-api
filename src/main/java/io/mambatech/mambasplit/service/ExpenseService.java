package io.mambatech.mambasplit.service;

import io.mambatech.mambasplit.domain.expense.Expense;
import io.mambatech.mambasplit.domain.expense.ExpenseSplit;
import io.mambatech.mambasplit.domain.money.Money;
import io.mambatech.mambasplit.repo.ExpenseRepository;
import io.mambatech.mambasplit.repo.ExpenseSplitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
public class ExpenseService {
  private final ExpenseRepository expenses;
  private final ExpenseSplitRepository splits;

  public ExpenseService(ExpenseRepository expenses, ExpenseSplitRepository splits) {
    this.expenses = expenses; this.splits = splits;
  }

  public record SplitExact(List<Item> items) {
    public record Item(UUID userId, long amountCents) {}
  }

  @Transactional
  public UUID createEqualSplitExpense(UUID groupId, UUID payerUserId, String description, long totalAmountCents, List<UUID> participants) {
    if (participants == null || participants.isEmpty()) throw new IllegalArgumentException("Participants required");
    if (totalAmountCents <= 0) throw new IllegalArgumentException("Amount must be > 0");

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

    return expenseId;
  }

  @Transactional
  public UUID createExactSplitExpense(UUID groupId, UUID payerUserId, String description, long totalAmountCents, List<SplitExact.Item> items) {
    if (items == null || items.isEmpty()) throw new IllegalArgumentException("Split items required");
    long sum = items.stream().mapToLong(SplitExact.Item::amountCents).sum();
    if (sum != totalAmountCents) throw new IllegalArgumentException("Split sum must equal total amount");

    UUID expenseId = UUID.randomUUID();
    expenses.save(new Expense(expenseId, groupId, payerUserId, description, Money.ofCents(totalAmountCents), Instant.now()));

    Set<UUID> seen = new HashSet<>();
    for (SplitExact.Item it : items) {
      if (!seen.add(it.userId())) throw new IllegalArgumentException("Duplicate user in split");
      if (it.amountCents() < 0) throw new IllegalArgumentException("Split amount cannot be negative");
      splits.save(new ExpenseSplit(UUID.randomUUID(), expenseId, it.userId(), Money.ofCents(it.amountCents())));
    }
    return expenseId;
  }
}
