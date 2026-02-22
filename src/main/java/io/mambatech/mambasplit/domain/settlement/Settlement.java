package io.mambatech.mambasplit.domain.settlement;

import io.mambatech.mambasplit.domain.money.Money;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name="settlements")
public class Settlement {
  @Id @Column(name="id", nullable=false) private UUID id;
  @Column(name="group_id", nullable=false) private UUID groupId;
  @Column(name="from_user_id", nullable=false) private UUID fromUserId;
  @Column(name="to_user_id", nullable=false) private UUID toUserId;
  @Column(name="amount_cents", nullable=false) private Money amount;
  @Column(name="note", length=500) private String note;
  @Column(name="created_at", nullable=false) private Instant createdAt;

  protected Settlement() {}

  public Settlement(UUID id, UUID groupId, UUID fromUserId, UUID toUserId, Money amount, String note, Instant createdAt) {
    this.id=id; this.groupId=groupId; this.fromUserId=fromUserId; this.toUserId=toUserId; this.amount=amount; this.note=note; this.createdAt=createdAt;
  }

  public UUID getId() { return id; }
  public UUID getGroupId() { return groupId; }
  public UUID getFromUserId() { return fromUserId; }
  public UUID getToUserId() { return toUserId; }
  public Money getAmount() { return amount; }
  public String getNote() { return note; }
  public Instant getCreatedAt() { return createdAt; }
}
