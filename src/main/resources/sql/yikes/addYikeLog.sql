INSERT INTO users
(guild_id, user_id, yike_count, user_quote_id)
VALUES ( ? , ? , 0, last_insert_rowid() )
ON CONFLICT DO NOTHING
RETURNING user_quote_id