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

alter table arena_gjennomforing
    add constraint fk_arena_tiltakskode foreign key (arena_tiltakskode) references tiltakstype (arena_tiltakskode);

create index idx_arena_gjennomforing_arena_tiltakskode on arena_gjennomforing (arena_tiltakskode);

alter table gjennomforing
    add constraint fk_tiltakskode foreign key (tiltakskode) references tiltakstype (tiltakskode);

create index idx_gjennomforing_tiltakskode on gjennomforing (tiltakskode);
