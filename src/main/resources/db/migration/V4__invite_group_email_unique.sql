WITH ranked_invites AS (
  SELECT
    id,
    row_number() OVER (
      PARTITION BY group_id, lower(email)
      ORDER BY created_at DESC, id DESC
    ) AS rn
  FROM invites
)
DELETE FROM invites i
USING ranked_invites r
WHERE i.id = r.id
  AND r.rn > 1;

CREATE UNIQUE INDEX uk_invites_group_email_ci ON invites(group_id, lower(email));
