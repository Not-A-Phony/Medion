-- Add username column allowing null initially
ALTER TABLE users ADD COLUMN username VARCHAR(255);

-- Backfill username using the part of the email before the @ symbol
UPDATE users SET username = split_part(email, '@', 1);

-- Handle any potential duplicates by appending a random string (just in case)
UPDATE users SET username = username || '_' || substring(md5(random()::text) from 1 for 4)
WHERE id IN (
    SELECT id FROM (
        SELECT id, ROW_NUMBER() OVER(PARTITION BY username ORDER BY id) as rn
        FROM users
    ) t WHERE rn > 1
);

-- Make it NOT NULL and UNIQUE
ALTER TABLE users ALTER COLUMN username SET NOT NULL;
ALTER TABLE users ADD CONSTRAINT unique_username UNIQUE (username);
