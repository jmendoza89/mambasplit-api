ALTER TABLE invites RENAME COLUMN token TO token_hash;
ALTER TABLE invites ALTER COLUMN token_hash TYPE VARCHAR(120);
