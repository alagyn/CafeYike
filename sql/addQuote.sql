INSERT INTO quotes
( user_id, content )
VALUES
( ? , ? )
RETURNING ROWID