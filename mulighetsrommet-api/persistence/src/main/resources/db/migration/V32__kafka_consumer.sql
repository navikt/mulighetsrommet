create table shedlock
(
    name       text         not null,
    lock_until timestamp    not null,
    locked_at  timestamp    not null,
    locked_by  varchar(255) not null,
    primary key (name)
);

create table failed_events
(
    id               serial  not null primary key,
    topic            text    not null,
    partition        integer not null,
    record_offset    bigint  not null,
    retries          integer not null default 0,
    last_retry       timestamp,
    key              bytea,
    value            bytea,
    headers_json     text,
    record_timestamp bigint,
    created_at       timestamp        default current_timestamp not null,
    unique (topic, partition, record_offset)
);

create type topic_type as enum ('CONSUMER', 'PRODUCER');

create table topics
(
    id      text       not null primary key,
    topic   text       not null,
    type    topic_type not null,
    running boolean default false
);
