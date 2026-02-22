package io.mambatech.mambasplit.domain.invite;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name="invites", uniqueConstraints=@UniqueConstraint(name="uk_invite_token_hash", columnNames={"token_hash"}))
public class Invite {
  @Id @Column(name="id", nullable=false) private UUID id;
  @Column(name="group_id", nullable=false) private UUID groupId;
  @Column(name="email", nullable=false, length=320) private String email;
  @Column(name="token_hash", nullable=false, length=120) private String tokenHash;
  @Column(name="expires_at", nullable=false) private Instant expiresAt;
  @Column(name="created_at", nullable=false) private Instant createdAt;

  protected Invite() {}

  public Invite(UUID id, UUID groupId, String email, String tokenHash, Instant expiresAt, Instant createdAt) {
    this.id=id; this.groupId=groupId; this.email=email; this.tokenHash=tokenHash; this.expiresAt=expiresAt; this.createdAt=createdAt;
  }

  public UUID getId() { return id; }
  public UUID getGroupId() { return groupId; }
  public String getEmail() { return email; }
  public String getTokenHash() { return tokenHash; }
  public Instant getExpiresAt() { return expiresAt; }
  public Instant getCreatedAt() { return createdAt; }
}
