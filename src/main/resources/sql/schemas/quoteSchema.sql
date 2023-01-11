CREATE TABLE IF NOT EXISTS quotes (
    quote_id INTEGER PRIMARY KEY AUTOINCREMENT,
    content TEXT NOT NULL,
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    user_ref INTEGER NOT NULL,
    FOREIGN KEY(user_ref) REFERENCES users(id)
)