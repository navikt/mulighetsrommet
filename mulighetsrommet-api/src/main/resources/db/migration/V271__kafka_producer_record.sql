create table kafka_producer_record
(
    id           bigint generated always as identity,
    topic        text not null,
    key          bytea,
    value        bytea,
    headers_json text,
    created_at   timestamp default current_timestamp not null
);
