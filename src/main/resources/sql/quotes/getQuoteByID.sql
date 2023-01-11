SELECT quotes.quote_id, users.id, quotes.content, quotes.created
FROM quotes
INNER JOIN users
ON users.id = quotes.user_ref
WHERE 
    quotes.quote_id = ?
