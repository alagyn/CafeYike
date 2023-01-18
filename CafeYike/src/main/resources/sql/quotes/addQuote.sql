INSERT INTO quotes
( user_ref, content )
SELECT id, ? FROM users
WHERE users.guild_id = ? AND users.user_id = ?
RETURNING quote_id