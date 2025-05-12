alter table arena_deltaker
    alter start_dato type date,
    alter slutt_dato type date,
    alter registrert_i_arena_dato type timestamptz using registrert_i_arena_dato at time zone 'Europe/Oslo',
    alter created_at type timestamptz using created_at at time zone 'Europe/Oslo',
    alter updated_at type timestamptz using updated_at at time zone 'Europe/Oslo';

alter table komet_deltaker
    alter status_opprettet_dato type timestamptz using status_opprettet_dato at time zone 'Europe/Oslo',
    alter registrert_dato type date,
    alter endret_dato type timestamptz using endret_dato at time zone 'Europe/Oslo',
    alter created_at type timestamptz using created_at at time zone 'Europe/Oslo',
    alter updated_at type timestamptz using updated_at at time zone 'Europe/Oslo';

alter table gruppetiltak
    add column created_at timestamptz default now(),
    add column updated_at timestamptz default now();

create trigger set_timestamp
    before update
    on gruppetiltak
    for each row
execute procedure trigger_set_timestamp();
