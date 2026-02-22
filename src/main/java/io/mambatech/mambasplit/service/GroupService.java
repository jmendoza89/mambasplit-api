package io.mambatech.mambasplit.service;

import io.mambatech.mambasplit.domain.group.Group;
import io.mambatech.mambasplit.domain.group.GroupMember;
import io.mambatech.mambasplit.domain.invite.Invite;
import io.mambatech.mambasplit.repo.GroupMemberRepository;
import io.mambatech.mambasplit.repo.GroupRepository;
import io.mambatech.mambasplit.repo.InviteRepository;
import io.mambatech.mambasplit.repo.UserRepository;
import io.mambatech.mambasplit.security.TokenCodec;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class GroupService {
  private final GroupRepository groups;
  private final GroupMemberRepository members;
  private final InviteRepository invites;
  private final UserRepository users;

  public GroupService(GroupRepository groups, GroupMemberRepository members, InviteRepository invites, UserRepository users) {
    this.groups = groups; this.members = members; this.invites = invites; this.users = users;
  }

  @Transactional
  public Group createGroup(UUID creatorUserId, String name) {
    Group g = new Group(UUID.randomUUID(), name, creatorUserId, Instant.now());
    groups.save(g);
    members.save(new GroupMember(UUID.randomUUID(), g.getId(), creatorUserId, "OWNER", Instant.now()));
    return g;
  }

  public List<Group> listGroupsForUser(UUID userId) {
    var memberships = members.findByUserId(userId);
    var ids = memberships.stream().map(GroupMember::getGroupId).toList();
    return groups.findAllById(ids);
  }

  public void requireMember(UUID groupId, UUID userId) {
    members.findByGroupIdAndUserId(groupId, userId).orElseThrow(() -> new IllegalArgumentException("Not a member of this group"));
  }

  public void requireMembers(UUID groupId, Iterable<UUID> userIds) {
    Set<UUID> distinctUserIds = new HashSet<>();
    for (UUID userId : userIds) {
      distinctUserIds.add(userId);
    }
    if (distinctUserIds.isEmpty()) {
      return;
    }
    long present = members.countByGroupIdAndUserIds(groupId, distinctUserIds);
    if (present != distinctUserIds.size()) {
      throw new IllegalArgumentException("One or more users are not members of this group");
    }
  }

  public record CreatedInvite(String token, String email, Instant expiresAt) {}

  @Transactional
  public CreatedInvite createInvite(UUID groupId, String email) {
    String normalizedEmail = normalizeInviteEmail(email);
    String token = TokenCodec.randomUrlToken(32);
    String tokenHash = TokenCodec.sha256Base64Url(token);
    Instant now = Instant.now();
    Instant expiresAt = now.plus(7, ChronoUnit.DAYS);
    invites.save(new Invite(UUID.randomUUID(), groupId, normalizedEmail, tokenHash, expiresAt, now));
    return new CreatedInvite(token, normalizedEmail, expiresAt);
  }

  @Transactional
  public void acceptInvite(String rawToken, UUID userId) {
    String tokenHash = TokenCodec.sha256Base64Url(rawToken);
    Invite invite = invites.findByTokenHash(tokenHash).orElseThrow(() -> new IllegalArgumentException("Invalid invite"));
    if (invite.getExpiresAt().isBefore(Instant.now())) throw new IllegalArgumentException("Invite expired");
    String userEmail = users.findById(userId)
      .orElseThrow(() -> new IllegalArgumentException("User not found"))
      .getEmail();
    if (!invite.getEmail().equalsIgnoreCase(userEmail)) {
      throw new IllegalArgumentException("Invite email does not match authenticated user");
    }
    long deleted = invites.deleteByTokenHash(tokenHash);
    if (deleted == 0) throw new IllegalArgumentException("Invite already used");
    members.findByGroupIdAndUserId(invite.getGroupId(), userId).orElseGet(() ->
      members.save(new GroupMember(UUID.randomUUID(), invite.getGroupId(), userId, "MEMBER", Instant.now()))
    );
  }

  private String normalizeInviteEmail(String email) {
    String normalized = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    if (normalized.isBlank()) {
      throw new IllegalArgumentException("Email is required");
    }
    return normalized;
  }
}
