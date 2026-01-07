-- V1.0.13__license_keys.sql
-- License keys for SPECIALIST_BUNDLE users to manage kids

-- ============================================================================
-- SPECIALIST BUNDLE ALLOCATION (tracks how many keys admin gave)
-- ============================================================================

CREATE TABLE specialist_bundle (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    specialist_user_id CHAR(36) NOT NULL UNIQUE,
    total_keys INT NOT NULL,
    assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    assigned_by CHAR(36) NOT NULL,
    notes TEXT NULL,
    CONSTRAINT fk_bundle_specialist FOREIGN KEY (specialist_user_id) REFERENCES users(id),
    CONSTRAINT fk_bundle_admin FOREIGN KEY (assigned_by) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================================
-- LICENSE KEYS TABLE
-- ============================================================================

CREATE TABLE license_key (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    key_uuid CHAR(36) NOT NULL UNIQUE,
    specialist_user_id CHAR(36) NOT NULL,
    profile_id BIGINT NULL,
    is_active BIT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    activated_at TIMESTAMP NULL,
    CONSTRAINT fk_license_specialist FOREIGN KEY (specialist_user_id) REFERENCES users(id),
    CONSTRAINT fk_license_profile FOREIGN KEY (profile_id) REFERENCES profile(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================================
-- HOMEWORK ASSIGNMENTS
-- ============================================================================

CREATE TABLE homework_assignment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    profile_id BIGINT NOT NULL,
    module_id BIGINT NULL,
    submodule_id BIGINT NULL,
    part_id BIGINT NULL,
    assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    due_date DATE NULL,
    notes TEXT NULL,
    CONSTRAINT fk_hw_profile FOREIGN KEY (profile_id) REFERENCES profile(id) ON DELETE CASCADE,
    CONSTRAINT fk_hw_module FOREIGN KEY (module_id) REFERENCES module(id),
    CONSTRAINT fk_hw_submodule FOREIGN KEY (submodule_id) REFERENCES submodule(id),
    CONSTRAINT fk_hw_part FOREIGN KEY (part_id) REFERENCES part(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================================
-- INDEXES FOR PERFORMANCE
-- ============================================================================

CREATE INDEX idx_license_key_uuid ON license_key(key_uuid);
CREATE INDEX idx_license_specialist ON license_key(specialist_user_id);
CREATE INDEX idx_license_profile ON license_key(profile_id);
CREATE INDEX idx_homework_profile ON homework_assignment(profile_id);
CREATE INDEX idx_bundle_specialist ON specialist_bundle(specialist_user_id);

