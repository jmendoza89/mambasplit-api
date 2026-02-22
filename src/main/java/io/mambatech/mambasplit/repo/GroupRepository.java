package io.mambatech.mambasplit.repo;

import io.mambatech.mambasplit.domain.group.Group;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface GroupRepository extends JpaRepository<Group, UUID> {}
