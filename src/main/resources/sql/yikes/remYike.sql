UPDATE users
SET yike_count = yike_count - 1
WHERE guild_id=? AND user_id=?
RETURNING yike_count
