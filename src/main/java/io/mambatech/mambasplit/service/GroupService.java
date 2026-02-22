package io.mambatech.mambasplit.service;

import io.mambatech.mambasplit.domain.group.Group;
import io.mambatech.mambasplit.domain.group.GroupMember;
import io.mambatech.mambasplit.domain.invite.Invite;
import io.mambatech.mambasplit.repo.GroupMemberRepository;
import io.mambatech.mambasplit.repo.GroupRepository;
import io.mambatech.mambasplit.repo.InviteRepository;
import io.mambatech.mambasplit.repo.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
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
    for (UUID userId : userIds) {
      requireMember(groupId, userId);
    }
  }

  @Transactional
  public Invite createInvite(UUID groupId, String email) {
    String token = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
    Instant now = Instant.now();
    return invites.save(new Invite(UUID.randomUUID(), groupId, email.toLowerCase(), token, now.plus(7, ChronoUnit.DAYS), now));
  }

  @Transactional
  public void acceptInvite(String token, UUID userId) {
    Invite invite = invites.findByToken(token).orElseThrow(() -> new IllegalArgumentException("Invalid invite"));
    if (invite.getExpiresAt().isBefore(Instant.now())) throw new IllegalArgumentException("Invite expired");
    String userEmail = users.findById(userId)
      .orElseThrow(() -> new IllegalArgumentException("User not found"))
      .getEmail();
    if (!invite.getEmail().equalsIgnoreCase(userEmail)) {
      throw new IllegalArgumentException("Invite email does not match authenticated user");
    }
    long deleted = invites.deleteByToken(token);
    if (deleted == 0) throw new IllegalArgumentException("Invite already used");
    members.findByGroupIdAndUserId(invite.getGroupId(), userId).orElseGet(() ->
      members.save(new GroupMember(UUID.randomUUID(), invite.getGroupId(), userId, "MEMBER", Instant.now()))
    );
  }
}
