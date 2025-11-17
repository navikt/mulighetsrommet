create table arena_gjennomforing
(
    id                           uuid        not null primary key,
    created_at                   timestamptz default now(),
    updated_at                   timestamptz default now(),
    arena_tiltakskode            text        not null,
    arena_reg_dato               timestamptz not null,
    arena_mod_dato               timestamptz not null,
    arrangor_organisasjonsnummer text        not null,
    navn                         text,
    deltidsprosent               numeric(5, 2)
);

create trigger set_timestamp
    before update
    on arena_gjennomforing
    for each row
execute procedure trigger_set_timestamp();
