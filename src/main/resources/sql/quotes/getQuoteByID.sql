SELECT quotes.quote_id, users.user_id, quotes.content, quotes.created
FROM quotes
INNER JOIN users
ON users.user_quote_id = quotes.user_ref
WHERE 
    quotes.quote_id = ?
