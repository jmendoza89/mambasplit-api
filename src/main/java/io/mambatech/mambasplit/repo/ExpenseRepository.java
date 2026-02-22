package io.mambatech.mambasplit.repo;

import io.mambatech.mambasplit.domain.expense.Expense;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ExpenseRepository extends JpaRepository<Expense, UUID> {
  List<Expense> findTop50ByGroupIdOrderByCreatedAtDesc(UUID groupId);
}
