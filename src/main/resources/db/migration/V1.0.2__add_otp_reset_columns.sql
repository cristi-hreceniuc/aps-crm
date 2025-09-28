ALTER TABLE `users`
  ADD COLUMN `otp_hash` VARCHAR(255) NULL AFTER `reset_token_expires_at`,
  ADD COLUMN `otp_expires_at` DATETIME NULL AFTER `otp_hash`,
  ADD COLUMN `otp_attempts` INT NOT NULL DEFAULT 0 AFTER `otp_expires_at`,
  ADD COLUMN `otp_locked_until` DATETIME NULL AFTER `otp_attempts`;

CREATE INDEX `ix_users_otp_expires_at` ON `users` (`otp_expires_at`);
CREATE INDEX `ix_users_otp_locked_until` ON `users` (`otp_locked_until`);
