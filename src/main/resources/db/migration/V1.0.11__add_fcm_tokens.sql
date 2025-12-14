-- Create table to store FCM tokens for push notifications
CREATE TABLE user_fcm_token (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    fcm_token VARCHAR(512) NOT NULL,
    device_info VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_used_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_fcm_token_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY unique_fcm_token (fcm_token)
);

-- Add index for faster lookups by user_id
CREATE INDEX idx_user_fcm_token_user_id ON user_fcm_token(user_id);

-- Add last_activity_at column to users table for tracking inactivity
ALTER TABLE users ADD COLUMN last_activity_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- Update existing users to have current timestamp as last_activity
UPDATE users SET last_activity_at = CURRENT_TIMESTAMP WHERE last_activity_at IS NULL;
