-- Table for storing pending user registrations awaiting OTP verification
CREATE TABLE pending_registrations (
    id VARCHAR(36) PRIMARY KEY,
    email VARCHAR(100) NOT NULL UNIQUE,
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    gender VARCHAR(50) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    user_role VARCHAR(50),
    otp_hash VARCHAR(255) NOT NULL,
    otp_expires_at TIMESTAMP NOT NULL,
    otp_attempts INT NOT NULL DEFAULT 0,
    otp_locked_until TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Index for email lookups
CREATE INDEX idx_pending_registrations_email ON pending_registrations(email);

-- Index for cleanup of expired registrations
CREATE INDEX idx_pending_registrations_expires ON pending_registrations(otp_expires_at);
