create table kafka_consumer_record
(
    id               bigint generated always as identity,
    topic            text      not null,
    partition        integer   not null,
    record_offset    bigint    not null,
    retries          integer   not null default 0,
    last_retry       timestamp,
    key              bytea,
    value            bytea,
    headers_json     text,
    record_timestamp bigint,
    created_at       timestamp not null default current_timestamp,
    unique (topic, partition, record_offset)
);

create table scheduled_tasks
(
    task_name            text                     not null,
    task_instance        text                     not null,
    task_data            bytea,
    execution_time       timestamp with time zone not null,
    picked               boolean                  not null,
    picked_by            text,
    last_success         timestamp with time zone,
    last_failure         timestamp with time zone,
    consecutive_failures int,
    last_heartbeat       timestamp with time zone,
    version              bigint                   not null,
    priority             smallint,
    primary key (task_name, task_instance)
);
