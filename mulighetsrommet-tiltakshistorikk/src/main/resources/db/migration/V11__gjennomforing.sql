create type gjennomforing_type as enum ('GRUPPE', 'ENKELTPLASS');

create table gjennomforing
(
    id                           uuid               not null primary key,
    created_at                   timestamptz default now(),
    updated_at                   timestamptz default now(),
    gjennomforing_type           gjennomforing_type not null,
    tiltakskode                  text               not null,
    arrangor_organisasjonsnummer text               not null,
    navn                         text,
    deltidsprosent               numeric(5, 2)
);

create trigger set_timestamp
    before update
    on gjennomforing
    for each row
execute procedure trigger_set_timestamp();
