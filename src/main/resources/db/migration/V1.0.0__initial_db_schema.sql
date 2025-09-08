CREATE TABLE users (
                       id          CHAR(36)      NOT NULL,
                       first_name  VARCHAR(100)  NOT NULL,
                       last_name   VARCHAR(100)  NOT NULL,
                       email       VARCHAR(255)  NOT NULL,
                       password    VARCHAR(255)  NOT NULL,
                       created_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       updated_at  DATETIME               DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                       gender      VARCHAR(20)   NOT NULL,
                       user_status VARCHAR(50)            DEFAULT NULL,
                       user_role   VARCHAR(50)            DEFAULT NULL,
                       PRIMARY KEY (id),
                       UNIQUE KEY uq_users_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
