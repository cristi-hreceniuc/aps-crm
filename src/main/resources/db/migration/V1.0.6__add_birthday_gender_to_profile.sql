-- V1.0.6__add_birthday_gender_to_profile.sql
-- Add birthday and gender columns to profile table

ALTER TABLE `profile`
  ADD COLUMN `birthday` DATE NULL AFTER `avatar_uri`,
  ADD COLUMN `gender` VARCHAR(20) NULL AFTER `birthday`;
