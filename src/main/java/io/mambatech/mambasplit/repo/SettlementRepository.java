package io.mambatech.mambasplit.repo;

import io.mambatech.mambasplit.domain.settlement.Settlement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SettlementRepository extends JpaRepository<Settlement, UUID> {
  List<Settlement> findTop50ByGroupIdOrderByCreatedAtDesc(UUID groupId);
}
