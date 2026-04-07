CREATE TABLE IF NOT EXISTS users (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    email       TEXT UNIQUE NOT NULL,
    password    TEXT NOT NULL,
    nickname    TEXT NOT NULL,
    role        TEXT NOT NULL DEFAULT 'USER',
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- Seed admin account (password: admin1234, BCrypt hash)
INSERT OR IGNORE INTO users (email, password, nickname, role)
VALUES ('admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '관리자', 'ADMIN');
