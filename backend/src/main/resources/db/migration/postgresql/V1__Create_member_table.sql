CREATE SEQUENCE IF NOT EXISTS member_id_seq;

CREATE TABLE IF NOT EXISTS member (
    id BIGINT PRIMARY KEY DEFAULT nextval('member_id_seq'),
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    phone_number VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

ALTER SEQUENCE member_id_seq OWNED BY member.id;

CREATE INDEX IF NOT EXISTS idx_member_email ON member(email);
CREATE INDEX IF NOT EXISTS idx_member_name ON member(name); 