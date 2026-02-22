package io.mambatech.mambasplit.repo;

import io.mambatech.mambasplit.domain.invite.Invite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface InviteRepository extends JpaRepository<Invite, UUID> {
  Optional<Invite> findByToken(String token);
  long deleteByToken(String token);
}
