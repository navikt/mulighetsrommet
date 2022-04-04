CREATE TABLE region(
    id          SERIAL PRIMARY KEY,
    navn        TEXT NOT NULL,
    kommune_ids INT[] NOT NULL
)