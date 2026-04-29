CREATE TABLE IF NOT EXISTS reviews (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id         INTEGER NOT NULL,
    order_id        INTEGER NOT NULL,
    menu_id         INTEGER NOT NULL,
    menu_name       TEXT NOT NULL,
    rating          INTEGER NOT NULL,
    content         TEXT NOT NULL,
    is_ai_generated INTEGER DEFAULT 0,
    admin_reply     TEXT,
    admin_reply_at  DATETIME,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP
);
