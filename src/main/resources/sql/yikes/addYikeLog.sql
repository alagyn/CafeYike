INSERT INTO users
(guild_id, user_id, yike_count, user_quote_id)
VALUES ( ? , ? , 0, (SELECT count(*) FROM users) )
ON CONFLICT DO NOTHING
RETURNING user_quote_id