INSERT INTO users
(guild_id, user_id, yike_count)
VALUES ( ? , ? , 1 )
ON CONFLICT DO
UPDATE SET yike_count = yike_count + 1
RETURNING yike_count
