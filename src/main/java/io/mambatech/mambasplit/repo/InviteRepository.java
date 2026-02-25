package io.mambatech.mambasplit.repo;

import io.mambatech.mambasplit.domain.invite.Invite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InviteRepository extends JpaRepository<Invite, UUID> {
  interface PendingInviteView {
    UUID getId();
    UUID getGroupId();
    String getGroupName();
    String getEmail();
    Instant getExpiresAt();
    Instant getCreatedAt();
  }

  Optional<Invite> findByTokenHash(String tokenHash);
  Optional<Invite> findByGroupIdAndEmailIgnoreCase(UUID groupId, String email);
  long deleteByTokenHash(String tokenHash);
  long deleteByIdAndTokenHash(UUID id, String tokenHash);
  long deleteByGroupIdAndTokenHash(UUID groupId, String tokenHash);

  @Query("""
      select i.id as id,
             i.groupId as groupId,
             g.name as groupName,
             i.email as email,
             i.expiresAt as expiresAt,
             i.createdAt as createdAt
      from Invite i
      join Group g on g.id = i.groupId
      where lower(i.email) = lower(:email)
        and i.expiresAt > :now
      order by i.createdAt desc
      """)
  List<PendingInviteView> findPendingByEmail(@Param("email") String email, @Param("now") Instant now);
}
