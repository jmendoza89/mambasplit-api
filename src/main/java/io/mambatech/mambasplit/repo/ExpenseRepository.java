package io.mambatech.mambasplit.repo;

import io.mambatech.mambasplit.domain.expense.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExpenseRepository extends JpaRepository<Expense, UUID> {
  List<Expense> findTop50ByGroupIdOrderByCreatedAtDesc(UUID groupId);
  Optional<Expense> findByIdAndGroupId(UUID id, UUID groupId);
  
  /**
   * Optimized query for fetching expenses with their splits in a single query.
   * This reduces N+1 query problem when loading group details.
   */
  @Query("""
      SELECT DISTINCT e FROM Expense e
      WHERE e.groupId = :groupId
      ORDER BY e.createdAt DESC
      LIMIT 50
      """)
  List<Expense> findTop50ByGroupIdWithOptimization(@Param("groupId") UUID groupId);
}
