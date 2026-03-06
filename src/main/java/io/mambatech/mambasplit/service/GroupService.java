package io.mambatech.mambasplit.service;

import io.mambatech.mambasplit.domain.group.Group;
import io.mambatech.mambasplit.domain.group.GroupMember;
import io.mambatech.mambasplit.domain.group.Role;
import io.mambatech.mambasplit.domain.invite.Invite;
import io.mambatech.mambasplit.exception.AuthorizationException;
import io.mambatech.mambasplit.exception.ConflictException;
import io.mambatech.mambasplit.exception.ResourceNotFoundException;
import io.mambatech.mambasplit.exception.ValidationException;
import io.mambatech.mambasplit.repo.ExpenseRepository;
import io.mambatech.mambasplit.repo.ExpenseSplitRepository;
import io.mambatech.mambasplit.repo.GroupMemberRepository;
import io.mambatech.mambasplit.repo.GroupRepository;
import io.mambatech.mambasplit.repo.InviteRepository;
import io.mambatech.mambasplit.repo.UserRepository;
import io.mambatech.mambasplit.security.TokenCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
  private static final Logger log = LoggerFactory.getLogger(GroupService.class);
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
    members.save(new GroupMember(UUID.randomUUID(), g.getId(), creatorUserId, Role.OWNER, Instant.now()));
    return g;
  }

  @Transactional(readOnly = true)
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
  public record MeInfo(UUID userId, Role role, long netBalanceCents) {}
  public record MemberInfo(UUID userId, String displayName, String email, Role role, Instant joinedAt, long netBalanceCents) {}
  public record ExpenseInfo(UUID id, String description, long amountCents, UUID payerUserId, Instant createdAt, List<ExpenseSplitInfo> splits) {}
  public record ExpenseSplitInfo(UUID userId, long amountOwedCents) {}
  public record Summary(int expenseCount, long totalExpenseAmountCents) {}

  @Transactional(readOnly = true)
  public GroupDetails getGroupDetails(UUID groupId, UUID userId) {
    GroupMember requesterMembership = members.findByGroupIdAndUserId(groupId, userId)
      .orElseThrow(() -> new AuthorizationException("access", "group " + groupId));
    Group group = groups.findById(groupId)
      .orElseThrow(() -> new ResourceNotFoundException("Group", groupId.toString()));

    // Data fetching optimized to minimize queries:
    // 1. Fetch all group members in one query
    List<GroupMember> groupMembers = members.findByGroupId(groupId);
    Set<UUID> memberUserIds = groupMembers.stream().map(GroupMember::getUserId).collect(java.util.stream.Collectors.toSet());
    
    // 2. Batch fetch all user details in one query (using IN clause)
    var usersById = users.findAllById(memberUserIds).stream().collect(java.util.stream.Collectors.toMap(u -> u.getId(), u -> u));

    // 3. Fetch expenses for the group
    List<io.mambatech.mambasplit.domain.expense.Expense> groupExpenses = expenses.findTop50ByGroupIdOrderByCreatedAtDesc(groupId);
    List<UUID> expenseIds = groupExpenses.stream().map(io.mambatech.mambasplit.domain.expense.Expense::getId).toList();
    
    // 4. Batch fetch all splits for these expenses in one query (using IN clause)
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
        if (user == null) {
          throw new ResourceNotFoundException("User", m.getUserId().toString());
        }
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
    Group group = groups.findById(groupId)
      .orElseThrow(() -> new ResourceNotFoundException("Group", groupId.toString()));
    if (!group.getCreatedBy().equals(actorUserId)) {
      log.warn("Authorization denied: User {} attempted to delete group {} owned by {}", 
               actorUserId, groupId, group.getCreatedBy());
      throw new AuthorizationException("delete", "group " + groupId);
    }
    groups.delete(group);
    log.info("Group deleted: groupId={}, deletedBy={}", groupId, actorUserId);
  }

  public void requireMember(UUID groupId, UUID userId) {
    members.findByGroupIdAndUserId(groupId, userId)
      .orElseThrow(() -> {
        log.warn("Authorization denied: User {} attempted to access group {} without membership", userId, groupId);
        return new AuthorizationException("access", "group " + groupId);
      });
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
      throw new ValidationException("One or more users are not members of group " + groupId);
    }
  }

  public record CreatedInvite(String token, String email, Instant expiresAt) {}
  public record PendingInvite(UUID id, UUID groupId, String groupName, String email, Instant expiresAt, Instant createdAt) {}

  @Transactional(readOnly = true)
  public List<PendingInvite> listPendingInvitesForEmail(String email, UUID requesterUserId) {
    String normalizedQueryEmail = normalizeInviteEmail(email);
    String requesterEmail = users.findById(requesterUserId)
      .orElseThrow(() -> new ResourceNotFoundException("User", requesterUserId.toString()))
      .getEmail();
    String normalizedRequesterEmail = normalizeInviteEmail(requesterEmail);
    if (!normalizedQueryEmail.equals(normalizedRequesterEmail)) {
      throw new AuthorizationException("list invites for another user");
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
        throw new ConflictException("User is already a member of this group");
      }
    });

    Instant now = Instant.now();
    invites.findByGroupIdAndEmailIgnoreCase(groupId, normalizedEmail).ifPresent(existingInvite -> {
      if (existingInvite.getExpiresAt().isAfter(now)) {
        throw new ConflictException("Invite already pending for this email in this group");
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
      throw new ValidationException("Invite token is required");
    }
    String tokenHash = TokenCodec.sha256Base64Url(rawToken);
    long deleted = invites.deleteByGroupIdAndTokenHash(groupId, tokenHash);
    if (deleted == 0) {
      throw new ResourceNotFoundException("Invite", "token hash");
    }
  }

  @Transactional
  public void acceptInvite(String rawToken, UUID userId) {
    String tokenHash = TokenCodec.sha256Base64Url(rawToken);
    Invite invite = invites.findByTokenHash(tokenHash)
      .orElseThrow(() -> new ResourceNotFoundException("Invite", "token"));
    
    // Check expiry - the window between this check and deletion is minimized by transaction
    if (invite.getExpiresAt().isBefore(Instant.now())) {
      throw new ValidationException("Invite has expired");
    }
    
    String userEmail = users.findById(userId)
      .orElseThrow(() -> new ResourceNotFoundException("User", userId.toString()))
      .getEmail();
    if (!invite.getEmail().equalsIgnoreCase(userEmail)) {
      throw new ValidationException("Invite email does not match authenticated user");
    }
    
    // Atomic deletion - if another thread deletes first, this returns 0
    long deleted = invites.deleteByTokenHash(tokenHash);
    if (deleted == 0) {
      throw new ConflictException("Invite already used");
    }
    
    // Add user as member (idempotent with orElseGet)
    members.findByGroupIdAndUserId(invite.getGroupId(), userId).orElseGet(() ->
      members.save(new GroupMember(UUID.randomUUID(), invite.getGroupId(), userId, Role.MEMBER, Instant.now()))
    );
  }

  @Transactional
  public void acceptInviteById(UUID inviteId, UUID userId) {
    Invite invite = invites.findById(inviteId)
      .orElseThrow(() -> new ResourceNotFoundException("Invite", inviteId.toString()));
    if (invite.getExpiresAt().isBefore(Instant.now())) {
      throw new ValidationException("Invite has expired");
    }
    String userEmail = users.findById(userId)
      .orElseThrow(() -> new ResourceNotFoundException("User", userId.toString()))
      .getEmail();
    if (!invite.getEmail().equalsIgnoreCase(userEmail)) {
      throw new ValidationException("Invite email does not match authenticated user");
    }
    long deleted = invites.deleteByIdAndTokenHash(invite.getId(), invite.getTokenHash());
    if (deleted == 0) {
      throw new ConflictException("Invite already used");
    }
    members.findByGroupIdAndUserId(invite.getGroupId(), userId).orElseGet(() ->
      members.save(new GroupMember(UUID.randomUUID(), invite.getGroupId(), userId, Role.MEMBER, Instant.now()))
    );
  }

  private String normalizeInviteEmail(String email) {
    String normalized = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    if (normalized.isBlank()) {
      throw new ValidationException("Email is required");
    }
    return normalized;
  }
}
