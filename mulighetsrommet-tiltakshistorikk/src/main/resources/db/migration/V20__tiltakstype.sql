create table tiltakstype
(
    id                int generated always as identity primary key,
    created_at        timestamptz default now(),
    updated_at        timestamptz default now(),
    arena_tiltakskode text unique,
    tiltakskode       text unique,
    navn              text
);

create trigger set_timestamp
    before update
    on tiltakstype
    for each row
execute procedure trigger_set_timestamp();
