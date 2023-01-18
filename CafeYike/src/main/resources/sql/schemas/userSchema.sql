CREATE TABLE IF NOT EXISTS users (
    id INTEGER PRIMARY KEY,
    guild_id INTEGER NOT NULL,
    user_id INTEGER NOT NULL,
    yike_count INTEGER NOT NULL DEFAULT 0,
    UNIQUE(guild_id, user_id)
)
