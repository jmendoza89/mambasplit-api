package io.mambatech.mambasplit.domain.expense;

import io.mambatech.mambasplit.domain.money.Money;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name="expenses")
public class Expense {
  @Id @Column(name="id", nullable=false) private UUID id;
  @Column(name="group_id", nullable=false) private UUID groupId;
  @Column(name="payer_user_id", nullable=false) private UUID payerUserId;
  @Column(name="description", nullable=false, length=300) private String description;
  @Column(name="amount_cents", nullable=false) private Money amount;
  @Column(name="created_at", nullable=false) private Instant createdAt;

  protected Expense() {}

  public Expense(UUID id, UUID groupId, UUID payerUserId, String description, Money amount, Instant createdAt) {
    this.id=id; this.groupId=groupId; this.payerUserId=payerUserId; this.description=description; this.amount=amount; this.createdAt=createdAt;
  }

  public UUID getId() { return id; }
  public UUID getGroupId() { return groupId; }
  public UUID getPayerUserId() { return payerUserId; }
  public String getDescription() { return description; }
  public Money getAmount() { return amount; }
  public Instant getCreatedAt() { return createdAt; }
}
