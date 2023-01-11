SELECT quotes.quote_id, users.id, quotes.content, quotes.created
FROM quotes
INNER JOIN users
ON users.id = quotes.user_ref
WHERE 
    users.guild_id = ?
    AND
    users.user_id = ?
