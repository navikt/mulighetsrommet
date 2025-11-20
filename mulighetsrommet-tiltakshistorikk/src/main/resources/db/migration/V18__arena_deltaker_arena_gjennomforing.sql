alter table arena_deltaker
    alter arena_gjennomforing_id set not null;

alter table arena_deltaker
    add constraint fk_arena_deltaker_arena_gjennomforing foreign key (arena_gjennomforing_id) references arena_gjennomforing (id);

create index idx_arena_deltaker_arena_gjennomforing_id on arena_deltaker (arena_gjennomforing_id);

create index idx_arena_gjennomforing_arrangor_organisasjonsnummer on arena_gjennomforing (arrangor_organisasjonsnummer);
