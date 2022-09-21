UPDATE yikes
SET count = count - 1
WHERE guild_id=? AND user_id=?
RETURNING count