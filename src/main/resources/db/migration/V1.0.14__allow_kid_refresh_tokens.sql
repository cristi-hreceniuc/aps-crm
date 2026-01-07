-- Drop foreign key constraint to allow kid refresh tokens
-- Kids authenticate with license keys and don't have user accounts
-- Their refresh tokens use "kid:{keyId}" format as user_id

ALTER TABLE refresh_token DROP FOREIGN KEY fk_rt_user;

-- Change user_id column type to varchar to accommodate "kid:*" format
ALTER TABLE refresh_token MODIFY COLUMN user_id VARCHAR(100) NOT NULL;

