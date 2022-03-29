CREATE TABLE events(
    id      SERIAL PRIMARY KEY,
    topic   TEXT NOT NULL,
    key     TEXT NOT NULL,
    "offset"  INTEGER NOT NULL,
    payload JSONB NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

ALTER TABLE events
    ADD CONSTRAINT events_unique UNIQUE (topic, key, "offset");
