package io.mambatech.mambasplit.domain.group;

/**
 * Group membership roles with hierarchical permissions.
 */
public enum Role {
  /**
   * Group owner with full control: can delete group, manage members, and manage expenses.
   */
  OWNER,
  
  /**
   * Regular group member: can create expenses and invites, view group details.
   */
  MEMBER
}
