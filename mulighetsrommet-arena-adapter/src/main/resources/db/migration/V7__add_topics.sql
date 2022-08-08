create type topic_type AS ENUM ('CONSUMER', 'PRODUCER');

create table if not exists topics
(
    id      serial primary key,
    key     text       not null,
    topic   text       not null,
    type    topic_type not null,
    running boolean default false,
    constraint topic_unique_key unique (key)
);
