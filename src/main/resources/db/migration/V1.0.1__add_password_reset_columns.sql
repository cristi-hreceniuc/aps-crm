-- V002__add_password_reset_columns.sql
-- Add password reset support fields

ALTER TABLE `users`
  ADD COLUMN `reset_token` VARCHAR(128) NULL AFTER `user_status`,
  ADD COLUMN `reset_token_expires_at` DATETIME NULL AFTER `reset_token`;

-- Token-ul va fi unic când este setat; în MySQL un UNIQUE permite mai multe NULL-uri
CREATE UNIQUE INDEX `ux_users_reset_token` ON `users` (`reset_token`);
CREATE INDEX `ix_users_reset_token_expires_at` ON `users` (`reset_token_expires_at`);
