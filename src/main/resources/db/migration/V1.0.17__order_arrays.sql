-- V1.0.17__order_arrays.sql
-- Move ordering from position columns to JSON arrays in parent entities.
-- This makes lesson insertion/deletion trivial without position renumbering.

-- ============================================================================
-- ADD ORDER ARRAY COLUMNS
-- ============================================================================

-- Part stores the order of its lessons
ALTER TABLE part ADD COLUMN lesson_order JSON;

-- Submodule stores the order of its parts
ALTER TABLE submodule ADD COLUMN part_order JSON;

-- Module stores the order of its submodules
ALTER TABLE module ADD COLUMN submodule_order JSON;

-- Lesson stores the order of its screens
ALTER TABLE lesson ADD COLUMN screen_order JSON;

-- ============================================================================
-- POPULATE ORDER ARRAYS FROM EXISTING POSITIONS
-- ============================================================================

-- Populate lesson_order in parts from existing lesson positions
UPDATE part p SET lesson_order = (
    SELECT JSON_ARRAYAGG(l.id ORDER BY l.position)
    FROM lesson l 
    WHERE l.part_id = p.id AND l.is_active = 1
);

-- Populate part_order in submodules from existing part positions
UPDATE submodule s SET part_order = (
    SELECT JSON_ARRAYAGG(p.id ORDER BY p.position)
    FROM part p 
    WHERE p.submodule_id = s.id AND p.is_active = 1
);

-- Populate submodule_order in modules from existing submodule positions
UPDATE module m SET submodule_order = (
    SELECT JSON_ARRAYAGG(s.id ORDER BY s.position)
    FROM submodule s 
    WHERE s.module_id = m.id AND s.is_active = 1
);

-- Populate screen_order in lessons from existing screen positions
UPDATE lesson l SET screen_order = (
    SELECT JSON_ARRAYAGG(ls.id ORDER BY ls.position)
    FROM lesson_screen ls 
    WHERE ls.lesson_id = l.id
);

-- ============================================================================
-- SET DEFAULT EMPTY ARRAYS FOR NULLS
-- ============================================================================

-- Ensure no NULL values (use empty array instead)
UPDATE part SET lesson_order = JSON_ARRAY() WHERE lesson_order IS NULL;
UPDATE submodule SET part_order = JSON_ARRAY() WHERE part_order IS NULL;
UPDATE module SET submodule_order = JSON_ARRAY() WHERE submodule_order IS NULL;
UPDATE lesson SET screen_order = JSON_ARRAY() WHERE screen_order IS NULL;

-- ============================================================================
-- NOTE: POSITION COLUMNS ARE KEPT FOR ROLLBACK SAFETY
-- They will be removed in a future migration (V1.0.18) after verification
-- ============================================================================

