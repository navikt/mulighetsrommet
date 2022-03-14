CREATE TABLE events(
    id      SERIAL PRIMARY KEY,
    topic   TEXT NOT NULL,
    key     TEXT NOT NULL,
    "offset"  INTEGER NOT NULL,
    payload JSONB NOT NULL
);

ALTER TABLE events
    ADD CONSTRAINT events_unique UNIQUE (topic, key, "offset");
