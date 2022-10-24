CREATE TABLE IF NOT EXISTS users (
    guild_id INTEGER NOT NULL,
    user_id INTEGER NOT NULL,
    yike_count INTEGER NOT NULL DEFAULT 0,
    user_quote_id INTEGER NOT NULL UNIQUE,
    PRIMARY KEY(guild_id, user_id)
)
