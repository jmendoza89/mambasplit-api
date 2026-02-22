package io.mambatech.mambasplit.repo;

import io.mambatech.mambasplit.domain.auth.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
  Optional<RefreshToken> findByTokenHash(String tokenHash);

  @Modifying
  @Query("""
      update RefreshToken rt
         set rt.revokedAt = :revokedAt
       where rt.tokenHash = :tokenHash
         and rt.revokedAt is null
         and rt.expiresAt > :now
      """)
  int revokeIfActive(@Param("tokenHash") String tokenHash, @Param("now") Instant now, @Param("revokedAt") Instant revokedAt);
}
