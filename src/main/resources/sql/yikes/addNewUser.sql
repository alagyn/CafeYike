INSERT INTO users
(guild_id, user_id, yike_count)
VALUES ( ? , ? , 0 )
ON CONFLICT DO NOTHING