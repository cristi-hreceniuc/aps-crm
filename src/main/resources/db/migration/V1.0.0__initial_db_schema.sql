-- Table: users
CREATE TABLE users
(
    id            UUID PRIMARY KEY,
    first_name    VARCHAR NOT NULL,
    last_name     VARCHAR NOT NULL,
    email         VARCHAR UNIQUE NOT NULL,
    password      VARCHAR NOT NULL,
    created_at    TIMESTAMP WITH TIME ZONE DEFAULT current_timestamp,
    updated_at    TIMESTAMP WITH TIME ZONE,
    gender        VARCHAR NOT NULL,
    status        VARCHAR,
    role          VARCHAR
);

