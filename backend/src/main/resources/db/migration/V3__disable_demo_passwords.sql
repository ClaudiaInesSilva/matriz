-- Demo accounts must not have usable credentials outside explicit local setup.
UPDATE users
SET password_hash = '!DISABLED!'
WHERE username IN ('demo.alex', 'demo.sam');

-- Keep any other existing passwordless accounts disabled as well.
UPDATE users
SET password_hash = '!DISABLED!'
WHERE password_hash IS NULL;

ALTER TABLE users
    ALTER COLUMN password_hash SET NOT NULL;
