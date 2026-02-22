-- Targeted cleanup for integration-test fixture users.
-- Assumes display_name values "User A" and "User B" are test-only.

CREATE TEMP TABLE tmp_test_user_ids ON COMMIT DROP AS
SELECT u.id
FROM users u
WHERE u.display_name IN ('User A', 'User B');

CREATE TEMP TABLE tmp_test_group_ids ON COMMIT DROP AS
SELECT DISTINCT g.id
FROM groups g
LEFT JOIN group_members gm ON gm.group_id = g.id
WHERE g.created_by IN (SELECT id FROM tmp_test_user_ids)
   OR gm.user_id IN (SELECT id FROM tmp_test_user_ids);

DELETE FROM refresh_tokens rt
WHERE rt.user_id IN (SELECT id FROM tmp_test_user_ids);

DELETE FROM settlements s
WHERE s.from_user_id IN (SELECT id FROM tmp_test_user_ids)
   OR s.to_user_id IN (SELECT id FROM tmp_test_user_ids)
   OR s.group_id IN (SELECT id FROM tmp_test_group_ids);

DELETE FROM expense_splits es
WHERE es.user_id IN (SELECT id FROM tmp_test_user_ids)
   OR es.expense_id IN (
        SELECT e.id
        FROM expenses e
        WHERE e.payer_user_id IN (SELECT id FROM tmp_test_user_ids)
           OR e.group_id IN (SELECT id FROM tmp_test_group_ids)
   );

DELETE FROM expenses e
WHERE e.payer_user_id IN (SELECT id FROM tmp_test_user_ids)
   OR e.group_id IN (SELECT id FROM tmp_test_group_ids);

DELETE FROM invites i
WHERE i.group_id IN (SELECT id FROM tmp_test_group_ids);

DELETE FROM group_members gm
WHERE gm.user_id IN (SELECT id FROM tmp_test_user_ids)
   OR gm.group_id IN (SELECT id FROM tmp_test_group_ids);

DELETE FROM groups g
WHERE g.id IN (SELECT id FROM tmp_test_group_ids);

DELETE FROM users u
WHERE u.id IN (SELECT id FROM tmp_test_user_ids);
