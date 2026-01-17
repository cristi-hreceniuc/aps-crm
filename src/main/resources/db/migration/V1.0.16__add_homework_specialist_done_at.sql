-- Add specialist "done/closed" timestamp for homework assignments
-- Allows a 2-step flow: kid completes -> specialist reviews and closes

ALTER TABLE homework_assignment
    ADD COLUMN specialist_done_at DATETIME NULL;


