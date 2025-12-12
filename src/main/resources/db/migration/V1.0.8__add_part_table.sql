-- V1.0.11__add_part_table.sql
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
-- CREATE PARTS FOR ALL SUBMODULES BASED ON EXISTING LESSON TYPES
-- ============================================================================

-- For each distinct submodule_id + lesson_type combination, create a part

-- INSTRUCTIONS parts
INSERT INTO part (submodule_id, name, slug, description, position, is_active)
SELECT DISTINCT
    l.submodule_id,
    'Instrucțiuni',
    'instructions',
    'Lecții de tip instructiv pentru introducerea sunetului',
    1,
    1
FROM lesson l
WHERE l.lesson_type = 'INSTRUCTIONS'
  AND NOT EXISTS (
    SELECT 1 FROM part p
    WHERE p.submodule_id = l.submodule_id AND p.slug = 'instructions'
);

-- IMAGE_SELECTION parts
INSERT INTO part (submodule_id, name, slug, description, position, is_active)
SELECT DISTINCT
    l.submodule_id,
    'Discriminare Imagini',
    'image-selection',
    'Exerciții de selectare și discriminare a imaginilor',
    2,
    1
FROM lesson l
WHERE l.lesson_type = 'IMAGE_SELECTION'
  AND NOT EXISTS (
    SELECT 1 FROM part p
    WHERE p.submodule_id = l.submodule_id AND p.slug = 'image-selection'
);

-- AUDIO_SELECTION parts
INSERT INTO part (submodule_id, name, slug, description, position, is_active)
SELECT DISTINCT
    l.submodule_id,
    'Discriminare Audio',
    'audio-selection',
    'Exerciții de ascultare și discriminare audio',
    3,
    1
FROM lesson l
WHERE l.lesson_type = 'AUDIO_SELECTION'
  AND NOT EXISTS (
    SELECT 1 FROM part p
    WHERE p.submodule_id = l.submodule_id AND p.slug = 'audio-selection'
);

-- SYLLABLE_SELECTION parts
INSERT INTO part (submodule_id, name, slug, description, position, is_active)
SELECT DISTINCT
    l.submodule_id,
    'Discriminare Silabe',
    'syllable-selection',
    'Exerciții de identificare și selectare a silabelor',
    4,
    1
FROM lesson l
WHERE l.lesson_type = 'SYLLABLE_SELECTION'
  AND NOT EXISTS (
    SELECT 1 FROM part p
    WHERE p.submodule_id = l.submodule_id AND p.slug = 'syllable-selection'
);

-- WORD_SELECTION parts
INSERT INTO part (submodule_id, name, slug, description, position, is_active)
SELECT DISTINCT
    l.submodule_id,
    'Discriminare Cuvinte',
    'word-selection',
    'Exerciții de identificare și selectare a cuvintelor',
    5,
    1
FROM lesson l
WHERE l.lesson_type = 'WORD_SELECTION'
  AND NOT EXISTS (
    SELECT 1 FROM part p
    WHERE p.submodule_id = l.submodule_id AND p.slug = 'word-selection'
);

-- FIND_SOUND parts
INSERT INTO part (submodule_id, name, slug, description, position, is_active)
SELECT DISTINCT
    l.submodule_id,
    'Găsește Sunetul',
    'find-sound',
    'Exerciții de identificare a sunetului în silabe',
    6,
    1
FROM lesson l
WHERE l.lesson_type = 'FIND_SOUND'
  AND NOT EXISTS (
    SELECT 1 FROM part p
    WHERE p.submodule_id = l.submodule_id AND p.slug = 'find-sound'
);

-- FIND_MISSING_LETTER parts
INSERT INTO part (submodule_id, name, slug, description, position, is_active)
SELECT DISTINCT
    l.submodule_id,
    'Litera Lipsă',
    'find-missing-letter',
    'Exerciții de completare cu litera lipsă',
    7,
    1
FROM lesson l
WHERE l.lesson_type = 'FIND_MISSING_LETTER'
  AND NOT EXISTS (
    SELECT 1 FROM part p
    WHERE p.submodule_id = l.submodule_id AND p.slug = 'find-missing-letter'
);

-- FIND_NON_INTRUDER parts
INSERT INTO part (submodule_id, name, slug, description, position, is_active)
SELECT DISTINCT
    l.submodule_id,
    'Găsește Asemănările',
    'find-non-intruder',
    'Exerciții de identificare a imaginilor asemănătoare',
    8,
    1
FROM lesson l
WHERE l.lesson_type = 'FIND_NON_INTRUDER'
  AND NOT EXISTS (
    SELECT 1 FROM part p
    WHERE p.submodule_id = l.submodule_id AND p.slug = 'find-non-intruder'
);

-- FORMAT_WORD parts
INSERT INTO part (submodule_id, name, slug, description, position, is_active)
SELECT DISTINCT
    l.submodule_id,
    'Formează Cuvântul',
    'format-word',
    'Exerciții de ordonare a literelor pentru a forma cuvinte',
    9,
    1
FROM lesson l
WHERE l.lesson_type = 'FORMAT_WORD'
  AND NOT EXISTS (
    SELECT 1 FROM part p
    WHERE p.submodule_id = l.submodule_id AND p.slug = 'format-word'
);

-- REPEAT_WORD parts (new type)
INSERT INTO part (submodule_id, name, slug, description, position, is_active)
SELECT DISTINCT
    l.submodule_id,
    'Repetă Cuvântul',
    'repeat-word',
    'Exerciții de pronunție și repetare a cuvintelor',
    10,
    1
FROM lesson l
WHERE l.lesson_type = 'REPEAT_WORD'
  AND NOT EXISTS (
    SELECT 1 FROM part p
    WHERE p.submodule_id = l.submodule_id AND p.slug = 'repeat-word'
);

-- FIND_SOUND_WITH_IMAGE parts (new type)
INSERT INTO part (submodule_id, name, slug, description, position, is_active)
SELECT DISTINCT
    l.submodule_id,
    'Găsește Sunetul (cu imagine)',
    'find-sound-image',
    'Exerciții de identificare a sunetului cu suport vizual',
    11,
    1
FROM lesson l
WHERE l.lesson_type = 'FIND_SOUND_WITH_IMAGE'
  AND NOT EXISTS (
    SELECT 1 FROM part p
    WHERE p.submodule_id = l.submodule_id AND p.slug = 'find-sound-image'
);

-- ============================================================================
-- LINK ALL LESSONS TO THEIR CORRESPONDING PARTS
-- ============================================================================

-- Link INSTRUCTIONS lessons
UPDATE lesson l
    INNER JOIN part p ON p.submodule_id = l.submodule_id AND p.slug = 'instructions'
SET l.part_id = p.id
WHERE l.lesson_type = 'INSTRUCTIONS';

-- Link IMAGE_SELECTION lessons
UPDATE lesson l
    INNER JOIN part p ON p.submodule_id = l.submodule_id AND p.slug = 'image-selection'
SET l.part_id = p.id
WHERE l.lesson_type = 'IMAGE_SELECTION';

-- Link AUDIO_SELECTION lessons
UPDATE lesson l
    INNER JOIN part p ON p.submodule_id = l.submodule_id AND p.slug = 'audio-selection'
SET l.part_id = p.id
WHERE l.lesson_type = 'AUDIO_SELECTION';

-- Link SYLLABLE_SELECTION lessons
UPDATE lesson l
    INNER JOIN part p ON p.submodule_id = l.submodule_id AND p.slug = 'syllable-selection'
SET l.part_id = p.id
WHERE l.lesson_type = 'SYLLABLE_SELECTION';

-- Link WORD_SELECTION lessons
UPDATE lesson l
    INNER JOIN part p ON p.submodule_id = l.submodule_id AND p.slug = 'word-selection'
SET l.part_id = p.id
WHERE l.lesson_type = 'WORD_SELECTION';

-- Link FIND_SOUND lessons
UPDATE lesson l
    INNER JOIN part p ON p.submodule_id = l.submodule_id AND p.slug = 'find-sound'
SET l.part_id = p.id
WHERE l.lesson_type = 'FIND_SOUND';

-- Link FIND_MISSING_LETTER lessons
UPDATE lesson l
    INNER JOIN part p ON p.submodule_id = l.submodule_id AND p.slug = 'find-missing-letter'
SET l.part_id = p.id
WHERE l.lesson_type = 'FIND_MISSING_LETTER';

-- Link FIND_NON_INTRUDER lessons
UPDATE lesson l
    INNER JOIN part p ON p.submodule_id = l.submodule_id AND p.slug = 'find-non-intruder'
SET l.part_id = p.id
WHERE l.lesson_type = 'FIND_NON_INTRUDER';

-- Link FORMAT_WORD lessons
UPDATE lesson l
    INNER JOIN part p ON p.submodule_id = l.submodule_id AND p.slug = 'format-word'
SET l.part_id = p.id
WHERE l.lesson_type = 'FORMAT_WORD';

-- Link REPEAT_WORD lessons
UPDATE lesson l
    INNER JOIN part p ON p.submodule_id = l.submodule_id AND p.slug = 'repeat-word'
SET l.part_id = p.id
WHERE l.lesson_type = 'REPEAT_WORD';

-- Link FIND_SOUND_WITH_IMAGE lessons
UPDATE lesson l
    INNER JOIN part p ON p.submodule_id = l.submodule_id AND p.slug = 'find-sound-image'
SET l.part_id = p.id
WHERE l.lesson_type = 'FIND_SOUND_WITH_IMAGE';

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
