-- V1.0.7__add_module_target_audience.sql
-- Add target_audience column to module table to distinguish between CHILDREN and SPECIALIST modules

ALTER TABLE `module`
  ADD COLUMN `target_audience` ENUM('CHILDREN', 'SPECIALIST') NOT NULL DEFAULT 'CHILDREN' AFTER `is_premium`;

-- Set existing modules to CHILDREN by default (already done by DEFAULT, but explicit for clarity)
UPDATE `module` SET `target_audience` = 'CHILDREN' WHERE `target_audience` IS NULL;

