package io.mambatech.mambasplit.domain.expense;

import io.mambatech.mambasplit.domain.money.Money;
import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name="expense_splits", uniqueConstraints=@UniqueConstraint(name="uk_expense_split_user", columnNames={"expense_id", "user_id"}))
public class ExpenseSplit {
  @Id @Column(name="id", nullable=false) private UUID id;
  @Column(name="expense_id", nullable=false) private UUID expenseId;
  @Column(name="user_id", nullable=false) private UUID userId;
  @Column(name="amount_owed_cents", nullable=false) private Money amountOwed;

  protected ExpenseSplit() {}

  public ExpenseSplit(UUID id, UUID expenseId, UUID userId, Money amountOwed) {
    this.id=id; this.expenseId=expenseId; this.userId=userId; this.amountOwed=amountOwed;
  }

  public UUID getId() { return id; }
  public UUID getExpenseId() { return expenseId; }
  public UUID getUserId() { return userId; }
  public Money getAmountOwed() { return amountOwed; }
}
