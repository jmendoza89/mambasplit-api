package io.mambatech.mambasplit.service;

import io.mambatech.mambasplit.domain.group.Group;
import io.mambatech.mambasplit.domain.group.GroupMember;
import io.mambatech.mambasplit.domain.invite.Invite;
import io.mambatech.mambasplit.repo.ExpenseRepository;
import io.mambatech.mambasplit.repo.ExpenseSplitRepository;
import io.mambatech.mambasplit.repo.GroupMemberRepository;
import io.mambatech.mambasplit.repo.GroupRepository;
import io.mambatech.mambasplit.repo.InviteRepository;
import io.mambatech.mambasplit.repo.UserRepository;
import io.mambatech.mambasplit.security.TokenCodec;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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
  private final ExpenseRepository expenses;
  private final ExpenseSplitRepository splits;
  private final UserRepository users;

  public GroupService(
    GroupRepository groups,
    GroupMemberRepository members,
    InviteRepository invites,
    ExpenseRepository expenses,
    ExpenseSplitRepository splits,
    UserRepository users
  ) {
    this.groups = groups;
    this.members = members;
    this.invites = invites;
    this.expenses = expenses;
    this.splits = splits;
    this.users = users;
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

  public record GroupDetails(
    GroupInfo group,
    MeInfo me,
    List<MemberInfo> members,
    List<ExpenseInfo> expenses,
    Summary summary
  ) {}

  public record GroupInfo(UUID id, String name, UUID createdBy, Instant createdAt) {}
  public record MeInfo(UUID userId, String role, long netBalanceCents) {}
  public record MemberInfo(UUID userId, String displayName, String email, String role, Instant joinedAt, long netBalanceCents) {}
  public record ExpenseInfo(UUID id, String description, long amountCents, UUID payerUserId, Instant createdAt, List<ExpenseSplitInfo> splits) {}
  public record ExpenseSplitInfo(UUID userId, long amountOwedCents) {}
  public record Summary(int expenseCount, long totalExpenseAmountCents) {}

  public GroupDetails getGroupDetails(UUID groupId, UUID userId) {
    GroupMember requesterMembership = members.findByGroupIdAndUserId(groupId, userId)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a member of this group"));
    Group group = groups.findById(groupId)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));

    List<GroupMember> groupMembers = members.findByGroupId(groupId);
    Set<UUID> memberUserIds = groupMembers.stream().map(GroupMember::getUserId).collect(java.util.stream.Collectors.toSet());
    var usersById = users.findAllById(memberUserIds).stream().collect(java.util.stream.Collectors.toMap(u -> u.getId(), u -> u));

    List<io.mambatech.mambasplit.domain.expense.Expense> groupExpenses = expenses.findTop50ByGroupIdOrderByCreatedAtDesc(groupId);
    List<UUID> expenseIds = groupExpenses.stream().map(io.mambatech.mambasplit.domain.expense.Expense::getId).toList();
    var splitsByExpenseId = expenseIds.isEmpty()
      ? java.util.Map.<UUID, List<io.mambatech.mambasplit.domain.expense.ExpenseSplit>>of()
      : splits.findByExpenseIdIn(expenseIds).stream()
        .collect(java.util.stream.Collectors.groupingBy(io.mambatech.mambasplit.domain.expense.ExpenseSplit::getExpenseId));

    java.util.Map<UUID, Long> netByUserId = new java.util.HashMap<>();
    for (UUID memberUserId : memberUserIds) {
      netByUserId.put(memberUserId, 0L);
    }
    for (var expense : groupExpenses) {
      netByUserId.merge(expense.getPayerUserId(), expense.getAmount().cents(), Long::sum);
      for (var split : splitsByExpenseId.getOrDefault(expense.getId(), java.util.List.of())) {
        netByUserId.merge(split.getUserId(), -split.getAmountOwed().cents(), Long::sum);
      }
    }

    List<MemberInfo> memberInfos = groupMembers.stream()
      .sorted(java.util.Comparator.comparing(GroupMember::getJoinedAt))
      .map(m -> {
        var user = usersById.get(m.getUserId());
        if (user == null) throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "User not found");
        return new MemberInfo(
          m.getUserId(),
          user.getDisplayName(),
          user.getEmail(),
          m.getRole(),
          m.getJoinedAt(),
          netByUserId.getOrDefault(m.getUserId(), 0L)
        );
      }).toList();

    List<ExpenseInfo> expenseInfos = groupExpenses.stream().map(e -> {
      List<ExpenseSplitInfo> splitInfos = splitsByExpenseId.getOrDefault(e.getId(), java.util.List.of()).stream()
        .sorted(java.util.Comparator.comparing(s -> s.getUserId().toString()))
        .map(s -> new ExpenseSplitInfo(s.getUserId(), s.getAmountOwed().cents()))
        .toList();
      return new ExpenseInfo(e.getId(), e.getDescription(), e.getAmount().cents(), e.getPayerUserId(), e.getCreatedAt(), splitInfos);
    }).toList();

    long totalExpenseAmountCents = groupExpenses.stream().mapToLong(e -> e.getAmount().cents()).sum();
    GroupInfo groupInfo = new GroupInfo(group.getId(), group.getName(), group.getCreatedBy(), group.getCreatedAt());
    MeInfo me = new MeInfo(userId, requesterMembership.getRole(), netByUserId.getOrDefault(userId, 0L));
    Summary summary = new Summary(expenseInfos.size(), totalExpenseAmountCents);
    return new GroupDetails(groupInfo, me, memberInfos, expenseInfos, summary);
  }

  @Transactional
  public void deleteGroup(UUID groupId, UUID actorUserId) {
    Group group = groups.findById(groupId).orElseThrow(() -> new IllegalArgumentException("Group not found"));
    if (!group.getCreatedBy().equals(actorUserId)) {
      throw new IllegalArgumentException("Only the group owner can delete this group");
    }
    groups.delete(group);
  }

  public void requireMember(UUID groupId, UUID userId) {
    members.findByGroupIdAndUserId(groupId, userId)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a member of this group"));
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
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "One or more users are not members of this group");
    }
  }

  public record CreatedInvite(String token, String email, Instant expiresAt) {}
  public record PendingInvite(UUID id, UUID groupId, String groupName, String email, Instant expiresAt, Instant createdAt) {}

  @Transactional(readOnly = true)
  public List<PendingInvite> listPendingInvitesForEmail(String email, UUID requesterUserId) {
    String normalizedQueryEmail = normalizeInviteEmail(email);
    String requesterEmail = users.findById(requesterUserId)
      .orElseThrow(() -> new IllegalArgumentException("User not found"))
      .getEmail();
    String normalizedRequesterEmail = normalizeInviteEmail(requesterEmail);
    if (!normalizedQueryEmail.equals(normalizedRequesterEmail)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot list invites for another user");
    }

    return invites.findPendingByEmail(normalizedQueryEmail, Instant.now()).stream()
      .map(i -> new PendingInvite(
        i.getId(),
        i.getGroupId(),
        i.getGroupName(),
        normalizeInviteEmail(i.getEmail()),
        i.getExpiresAt(),
        i.getCreatedAt()
      ))
      .toList();
  }

  @Transactional
  public CreatedInvite createInvite(UUID groupId, String email) {
    String normalizedEmail = normalizeInviteEmail(email);
    users.findByEmailIgnoreCase(normalizedEmail).ifPresent(user -> {
      if (members.findByGroupIdAndUserId(groupId, user.getId()).isPresent()) {
        throw new ResponseStatusException(HttpStatus.CONFLICT, "User is already a member of this group");
      }
    });

    Instant now = Instant.now();
    invites.findByGroupIdAndEmailIgnoreCase(groupId, normalizedEmail).ifPresent(existingInvite -> {
      if (existingInvite.getExpiresAt().isAfter(now)) {
        throw new ResponseStatusException(HttpStatus.CONFLICT, "Invite already pending for this email in this group");
      }
      invites.delete(existingInvite);
    });

    String token = TokenCodec.randomUrlToken(32);
    String tokenHash = TokenCodec.sha256Base64Url(token);
    Instant expiresAt = now.plus(7, ChronoUnit.DAYS);
    invites.save(new Invite(UUID.randomUUID(), groupId, normalizedEmail, tokenHash, expiresAt, now));
    return new CreatedInvite(token, normalizedEmail, expiresAt);
  }

  @Transactional
  public void cancelInvite(UUID groupId, String rawToken, UUID actorUserId) {
    requireMember(groupId, actorUserId);
    if (rawToken == null || rawToken.isBlank()) {
      throw new IllegalArgumentException("Invite token is required");
    }
    String tokenHash = TokenCodec.sha256Base64Url(rawToken);
    long deleted = invites.deleteByGroupIdAndTokenHash(groupId, tokenHash);
    if (deleted == 0) {
      throw new IllegalArgumentException("Invite not found");
    }
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

  @Transactional
  public void acceptInviteById(UUID inviteId, UUID userId) {
    Invite invite = invites.findById(inviteId).orElseThrow(() -> new IllegalArgumentException("Invalid invite"));
    if (invite.getExpiresAt().isBefore(Instant.now())) throw new IllegalArgumentException("Invite expired");
    String userEmail = users.findById(userId)
      .orElseThrow(() -> new IllegalArgumentException("User not found"))
      .getEmail();
    if (!invite.getEmail().equalsIgnoreCase(userEmail)) {
      throw new IllegalArgumentException("Invite email does not match authenticated user");
    }
    long deleted = invites.deleteByIdAndTokenHash(invite.getId(), invite.getTokenHash());
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
