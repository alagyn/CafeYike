INSERT INTO users
(guild_id, user_id, yike_count, user_quote_id)
VALUES ( ? , ? , 1, (SELECT count(*) FROM users) )
ON CONFLICT DO
UPDATE SET yike_count = yike_count + 1
RETURNING yike_count
