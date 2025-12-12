-- V1.0.9__cleanup_lesson_titles.sql
-- Remove redundant type prefixes from lesson titles now that we have parts grouping them

-- ============================================================================
-- CLEAN UP LESSON TITLES - Remove type prefixes
-- ============================================================================

-- Remove "Discriminare imagine - " prefix
UPDATE lesson 
SET title = TRIM(SUBSTRING_INDEX(title, ' - ', -1))
WHERE title LIKE 'Discriminare imagine - %';

-- Remove "Discriminare audio - " prefix
UPDATE lesson 
SET title = TRIM(SUBSTRING_INDEX(title, ' - ', -1))
WHERE title LIKE 'Discriminare audio - %';

-- Remove "Discriminare silabe - " prefix  
UPDATE lesson 
SET title = TRIM(SUBSTRING_INDEX(title, ' - ', -1))
WHERE title LIKE 'Discriminare silabe - %';

-- Remove "Discriminare cuvinte - " prefix
UPDATE lesson 
SET title = TRIM(SUBSTRING_INDEX(title, ' - ', -1))
WHERE title LIKE 'Discriminare cuvinte - %';

-- Remove "Discriminare - " prefix (generic catch-all)
UPDATE lesson 
SET title = TRIM(SUBSTRING_INDEX(title, ' - ', -1))
WHERE title LIKE 'Discriminare - %';

-- Simplify introduction lessons - keep them as is or simplify if needed
-- UPDATE lesson 
-- SET title = 'Introducere'
-- WHERE title LIKE 'Introducere sunetul %';

-- ============================================================================
-- VERIFICATION (commented out - uncomment to test)
-- ============================================================================

-- Check updated titles:
-- SELECT id, code, title, lesson_type, part_id
-- FROM lesson
-- ORDER BY position;
