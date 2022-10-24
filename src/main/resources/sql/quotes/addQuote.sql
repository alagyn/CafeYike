INSERT INTO quotes
( user_ref, content )
VALUES
( ? , ? )
RETURNING quote_id