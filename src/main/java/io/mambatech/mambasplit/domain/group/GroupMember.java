package io.mambatech.mambasplit.domain.group;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name="group_members", uniqueConstraints=@UniqueConstraint(name="uk_group_member", columnNames={"group_id", "user_id"}))
public class GroupMember {
  @Id @Column(name="id", nullable=false) private UUID id;
  @Column(name="group_id", nullable=false) private UUID groupId;
  @Column(name="user_id", nullable=false) private UUID userId;
  @Column(name="role", nullable=false, length=30) private String role;
  @Column(name="joined_at", nullable=false) private Instant joinedAt;

  protected GroupMember() {}

  public GroupMember(UUID id, UUID groupId, UUID userId, String role, Instant joinedAt) {
    this.id=id; this.groupId=groupId; this.userId=userId; this.role=role; this.joinedAt=joinedAt;
  }

  public UUID getId() { return id; }
  public UUID getGroupId() { return groupId; }
  public UUID getUserId() { return userId; }
  public String getRole() { return role; }
  public Instant getJoinedAt() { return joinedAt; }
}
