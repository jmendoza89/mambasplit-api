package io.mambatech.mambasplit.repo;

import io.mambatech.mambasplit.domain.invite.Invite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface InviteRepository extends JpaRepository<Invite, UUID> {
  Optional<Invite> findByTokenHash(String tokenHash);
  long deleteByTokenHash(String tokenHash);
}
