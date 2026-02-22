package io.mambatech.mambasplit.domain.group;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "groups")
public class Group {
  @Id @Column(name="id", nullable=false) private UUID id;
  @Column(name="name", nullable=false, length=200) private String name;
  @Column(name="created_by", nullable=false) private UUID createdBy;
  @Column(name="created_at", nullable=false) private Instant createdAt;

  protected Group() {}

  public Group(UUID id, String name, UUID createdBy, Instant createdAt) {
    this.id=id; this.name=name; this.createdBy=createdBy; this.createdAt=createdAt;
  }

  public UUID getId() { return id; }
  public String getName() { return name; }
  public UUID getCreatedBy() { return createdBy; }
  public Instant getCreatedAt() { return createdAt; }
  public void setName(String name) { this.name = name; }
}
