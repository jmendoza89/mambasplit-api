package io.mambatech.mambasplit.domain.user;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {
  @Id @Column(name="id", nullable=false) private UUID id;
  @Column(name="email", nullable=false, unique=true, length=320) private String email;
  @Column(name="password_hash", nullable=false, length=200) private String passwordHash;
  @Column(name="display_name", nullable=false, length=120) private String displayName;
  @Column(name="created_at", nullable=false) private Instant createdAt;

  protected User() {}

  public User(UUID id, String email, String passwordHash, String displayName, Instant createdAt) {
    this.id=id; this.email=email; this.passwordHash=passwordHash; this.displayName=displayName; this.createdAt=createdAt;
  }

  public UUID getId() { return id; }
  public String getEmail() { return email; }
  public String getPasswordHash() { return passwordHash; }
  public String getDisplayName() { return displayName; }
  public Instant getCreatedAt() { return createdAt; }
  public void setDisplayName(String displayName) { this.displayName = displayName; }
}
