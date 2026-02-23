package io.mambatech.mambasplit.repo;

import io.mambatech.mambasplit.domain.expense.ExpenseSplit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ExpenseSplitRepository extends JpaRepository<ExpenseSplit, UUID> {
  List<ExpenseSplit> findByExpenseId(UUID expenseId);
  List<ExpenseSplit> findByExpenseIdIn(List<UUID> expenseIds);
}
