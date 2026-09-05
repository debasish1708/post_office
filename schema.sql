-- Same schema as src/main/resources/schema.sql — run against database postoffice if needed.

CREATE TABLE IF NOT EXISTS users (
    id            BIGSERIAL PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    email         VARCHAR(150) NOT NULL UNIQUE,
    password      VARCHAR(255) NOT NULL,
    balance       NUMERIC(12, 2) NOT NULL DEFAULT 100.00,
    created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at    TIMESTAMP
);

CREATE TABLE IF NOT EXISTS service (
    id            BIGSERIAL PRIMARY KEY,
    name          VARCHAR(50) NOT NULL,
    slug          VARCHAR(50) NOT NULL UNIQUE,
    charges       NUMERIC(12, 2) NOT NULL,
    delivery_min  INTEGER NOT NULL,
    created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at    TIMESTAMP
);

CREATE TABLE IF NOT EXISTS post_office (
    id                  BIGSERIAL PRIMARY KEY,
    tracking_id         VARCHAR(40) NOT NULL UNIQUE,
    sender_id           BIGINT NOT NULL REFERENCES users(id),
    receiver_id         BIGINT NOT NULL REFERENCES users(id),
    post_date           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    message             TEXT,
    letter_image        VARCHAR(255) NOT NULL,
    attachment_image    VARCHAR(255),
    service_id          BIGINT NOT NULL REFERENCES service(id),
    total_cost          NUMERIC(12, 2) NOT NULL,
    receiving_date      TIMESTAMP NOT NULL,
    curr_node_address   VARCHAR(150) NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'start',
    unlock_otp          VARCHAR(6) NOT NULL,
    is_read             BOOLEAN NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at          TIMESTAMP,
    CONSTRAINT chk_not_self CHECK (sender_id <> receiver_id)
);

CREATE TABLE IF NOT EXISTS wallet_ledger (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL REFERENCES users(id),
    letter_id   BIGINT REFERENCES post_office(id),
    amount      NUMERIC(12, 2) NOT NULL,
    type        VARCHAR(30) NOT NULL,
    description VARCHAR(255) NOT NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
