alter table arena_deltaker
    alter arena_mod_dato set not null;

alter table arena_deltaker
    add arena_gjennomforing_id uuid;
