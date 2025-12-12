-- V1.0.8__add_part_table.sql
-- Add 'part' table to create hierarchy: module → submodule → part → lesson
-- This allows grouping lessons by type/category within each submodule

-- ============================================================================
-- CREATE PART TABLE
-- ============================================================================

CREATE TABLE part (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    submodule_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(120) NOT NULL,
    description TEXT,
    position INT NOT NULL,
    is_active BIT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_part_submodule FOREIGN KEY (submodule_id) REFERENCES submodule(id),
    CONSTRAINT uq_part_slug_per_submodule UNIQUE (submodule_id, slug)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================================
-- ADD part_id TO LESSON TABLE
-- ============================================================================

ALTER TABLE lesson
    ADD COLUMN part_id BIGINT NULL AFTER submodule_id,
    ADD CONSTRAINT fk_lesson_part FOREIGN KEY (part_id) REFERENCES part(id);

-- ============================================================================
-- MIGRATE EXISTING DATA: Create parts based on lesson_type
-- ============================================================================
-- This script will:
-- 1. Find all unique lesson_type values per submodule
-- 2. Create a 'part' for each lesson_type
-- 3. Link lessons to their corresponding part

-- Get the submodule ID for 'S' submodule (from the previous migration)
SET @s_submodule_id = (SELECT id FROM submodule WHERE slug = 's' LIMIT 1);

-- Only proceed if we have lessons to migrate
SET @has_lessons = (SELECT COUNT(*) FROM lesson WHERE submodule_id = @s_submodule_id);

-- Create parts for the 'S' submodule based on lesson types
-- Part 1: Instructions (INSTRUCTIONS)
INSERT INTO part (submodule_id, name, slug, description, position, is_active)
SELECT 
    @s_submodule_id,
    'Instrucțiuni',
    'instructions',
    'Lecții de tip instructiv pentru introducerea sunetului',
    1,
    1
FROM dual
WHERE EXISTS (SELECT 1 FROM lesson WHERE submodule_id = @s_submodule_id AND lesson_type = 'INSTRUCTIONS');

SET @part_instructions_id = LAST_INSERT_ID();

-- Part 2: Image Selection (IMAGE_SELECTION)
INSERT INTO part (submodule_id, name, slug, description, position, is_active)
SELECT 
    @s_submodule_id,
    'Discriminare Imagini',
    'image-selection',
    'Exerciții de selectare și discriminare a imaginilor',
    2,
    1
FROM dual
WHERE EXISTS (SELECT 1 FROM lesson WHERE submodule_id = @s_submodule_id AND lesson_type = 'IMAGE_SELECTION');

SET @part_image_selection_id = LAST_INSERT_ID();

-- Part 3: Audio Selection (AUDIO_SELECTION)
INSERT INTO part (submodule_id, name, slug, description, position, is_active)
SELECT 
    @s_submodule_id,
    'Discriminare Audio',
    'audio-selection',
    'Exerciții de ascultare și discriminare audio',
    3,
    1
FROM dual
WHERE EXISTS (SELECT 1 FROM lesson WHERE submodule_id = @s_submodule_id AND lesson_type = 'AUDIO_SELECTION');

SET @part_audio_selection_id = LAST_INSERT_ID();

-- Part 4: Syllable Selection (SYLLABLE_SELECTION)
INSERT INTO part (submodule_id, name, slug, description, position, is_active)
SELECT 
    @s_submodule_id,
    'Discriminare Silabe',
    'syllable-selection',
    'Exerciții de identificare și selectare a silabelor',
    4,
    1
FROM dual
WHERE EXISTS (SELECT 1 FROM lesson WHERE submodule_id = @s_submodule_id AND lesson_type = 'SYLLABLE_SELECTION');

SET @part_syllable_selection_id = LAST_INSERT_ID();

-- Part 5: Word Selection (WORD_SELECTION)
INSERT INTO part (submodule_id, name, slug, description, position, is_active)
SELECT 
    @s_submodule_id,
    'Discriminare Cuvinte',
    'word-selection',
    'Exerciții de identificare și selectare a cuvintelor',
    5,
    1
FROM dual
WHERE EXISTS (SELECT 1 FROM lesson WHERE submodule_id = @s_submodule_id AND lesson_type = 'WORD_SELECTION');

SET @part_word_selection_id = LAST_INSERT_ID();

-- ============================================================================
-- LINK EXISTING LESSONS TO PARTS
-- ============================================================================

-- Link INSTRUCTIONS lessons
UPDATE lesson 
SET part_id = (SELECT id FROM part WHERE submodule_id = @s_submodule_id AND slug = 'instructions' LIMIT 1)
WHERE submodule_id = @s_submodule_id AND lesson_type = 'INSTRUCTIONS';

-- Link IMAGE_SELECTION lessons
UPDATE lesson 
SET part_id = (SELECT id FROM part WHERE submodule_id = @s_submodule_id AND slug = 'image-selection' LIMIT 1)
WHERE submodule_id = @s_submodule_id AND lesson_type = 'IMAGE_SELECTION';

-- Link AUDIO_SELECTION lessons
UPDATE lesson 
SET part_id = (SELECT id FROM part WHERE submodule_id = @s_submodule_id AND slug = 'audio-selection' LIMIT 1)
WHERE submodule_id = @s_submodule_id AND lesson_type = 'AUDIO_SELECTION';

-- Link SYLLABLE_SELECTION lessons
UPDATE lesson 
SET part_id = (SELECT id FROM part WHERE submodule_id = @s_submodule_id AND slug = 'syllable-selection' LIMIT 1)
WHERE submodule_id = @s_submodule_id AND lesson_type = 'SYLLABLE_SELECTION';

-- Link WORD_SELECTION lessons
UPDATE lesson 
SET part_id = (SELECT id FROM part WHERE submodule_id = @s_submodule_id AND slug = 'word-selection' LIMIT 1)
WHERE submodule_id = @s_submodule_id AND lesson_type = 'WORD_SELECTION';

-- ============================================================================
-- MAKE part_id REQUIRED (after data migration)
-- ============================================================================
-- After all existing lessons are linked to parts, make part_id mandatory for future lessons

ALTER TABLE lesson
    MODIFY COLUMN part_id BIGINT NOT NULL;

-- ============================================================================
-- CREATE INDEX FOR PERFORMANCE
-- ============================================================================

CREATE INDEX idx_part_submodule ON part(submodule_id);
CREATE INDEX idx_lesson_part ON lesson(part_id);

-- ============================================================================
-- VERIFICATION QUERIES (commented out - uncomment to test)
-- ============================================================================

-- Check parts created:
-- SELECT p.id, p.name, p.slug, p.position, s.title as submodule_title
-- FROM part p
-- JOIN submodule s ON p.submodule_id = s.id
-- ORDER BY s.id, p.position;

-- Check lessons linked to parts:
-- SELECT 
--     m.title as module,
--     s.title as submodule,
--     p.name as part,
--     COUNT(l.id) as lesson_count
-- FROM lesson l
-- JOIN part p ON l.part_id = p.id
-- JOIN submodule s ON p.submodule_id = s.id
-- JOIN module m ON s.module_id = m.id
-- GROUP BY m.title, s.title, p.name
-- ORDER BY m.id, s.id, p.position;
