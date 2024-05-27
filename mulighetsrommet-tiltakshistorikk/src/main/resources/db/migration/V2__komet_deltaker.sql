create table komet_deltaker
(
    id                           uuid                    not null primary key,
    gjennomforing_id             uuid                    not null,
    person_ident                 text                    not null,
    start_dato                   date,
    slutt_dato                   date,
    status_type                  text                    not null,
    status_opprettet_dato        timestamp               not null,
    status_aarsak                text,
    registrert_dato              timestamp               not null,
    endret_dato                  timestamp               not null,
    dager_per_uke                real,
    prosent_stilling             real,
    created_at                   timestamp default now() not null,
    updated_at                   timestamp default now() not null
);

create trigger set_timestamp
    before update
    on komet_deltaker
    for each row
execute procedure trigger_set_timestamp();
