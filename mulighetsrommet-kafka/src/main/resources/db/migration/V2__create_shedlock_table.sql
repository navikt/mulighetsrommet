CREATE TABLE shedlock
(
    name       TEXT         NOT NULL,
    lock_until TIMESTAMP    NOT NULL,
    locked_at  TIMESTAMP    NOT NULL,
    locked_by  VARCHAR(255) NOT NULL,
    PRIMARY KEY (name)
);


CREATE TABLE failed_events
(
    id               SERIAL  NOT NULL PRIMARY KEY,
    topic            TEXT    NOT NULL,
    partition        INTEGER NOT NULL,
    record_offset    BIGINT  NOT NULL,
    retries          INTEGER NOT NULL DEFAULT 0,
    last_retry       TIMESTAMP,
    key              BYTEA,
    value            BYTEA,
    headers_json     TEXT,
    record_timestamp BIGINT,
    created_at       TIMESTAMP        DEFAULT CURRENT_TIMESTAMP NOT NULL,
    UNIQUE (topic, partition, record_offset)
);
