SELECT
    quotes.quote_id AS quote_id,
    users.user_id AS user_id,
    quotes.content AS content,
    quotes.created AS created
FROM quotes
INNER JOIN users
ON users.id = quotes.user_ref
WHERE 
    users.guild_id = ?
    AND
    users.user_id = ?
