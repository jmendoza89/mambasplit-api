package io.mambatech.mambasplit.repo;

import io.mambatech.mambasplit.domain.group.GroupMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface GroupMemberRepository extends JpaRepository<GroupMember, UUID> {
  List<GroupMember> findByUserId(UUID userId);
  Optional<GroupMember> findByGroupIdAndUserId(UUID groupId, UUID userId);

  @Query("""
      select count(gm.userId)
      from GroupMember gm
      where gm.groupId = :groupId and gm.userId in :userIds
      """)
  long countByGroupIdAndUserIds(@Param("groupId") UUID groupId, @Param("userIds") Set<UUID> userIds);
}
