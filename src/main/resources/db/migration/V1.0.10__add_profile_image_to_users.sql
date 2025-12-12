-- V1.0.10__add_profile_image_to_users.sql
-- Add profile_image_url column to users table

ALTER TABLE `users`
  ADD COLUMN `profile_image_url` VARCHAR(500) NULL AFTER `is_premium`;
