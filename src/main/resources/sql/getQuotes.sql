SELECT quote_id, user_id, content, created
FROM quotes
WHERE user_id IN ( ? )
