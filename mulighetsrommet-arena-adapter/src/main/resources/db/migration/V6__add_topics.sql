create type topic_type AS ENUM ('CONSUMER', 'PRODUCER');

create table if not exists topics
(
    id      serial primary key,
    name    text        not null,
    topic   text        not null,
    type    topic_type not null,
    running boolean default false
);
