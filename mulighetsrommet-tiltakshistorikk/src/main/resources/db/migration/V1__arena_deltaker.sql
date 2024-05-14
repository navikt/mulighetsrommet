create or replace function trigger_set_timestamp()
    returns trigger as
$$
begin
    new.updated_at = now();
    return new;
end;
$$ language plpgsql;

create type arena_deltaker_status as enum (
    'AVSLAG',
    'IKKE_AKTUELL',
    'TAKKET_NEI_TIL_TILBUD',
    'TILBUD',
    'TAKKET_JA_TIL_TILBUD',
    'INFORMASJONSMOTE',
    'AKTUELL',
    'VENTELISTE',
    'GJENNOMFORES',
    'DELTAKELSE_AVBRUTT',
    'GJENNOMFORING_AVBRUTT',
    'GJENNOMFORING_AVLYST',
    'FULLFORT',
    'IKKE_MOTT');

create table arena_deltaker
(
    id                           uuid                    not null primary key,
    norsk_ident                  varchar(11)             not null,
    arena_tiltakskode            text                    not null,
    status                       arena_deltaker_status   not null,
    start_dato                   timestamp,
    slutt_dato                   timestamp,
    beskrivelse                  text                    not null,
    arrangor_organisasjonsnummer text                    not null,
    registrert_i_arena_dato      timestamp               not null,
    created_at                   timestamp default now() not null,
    updated_at                   timestamp default now() not null
);

create index arena_deltaker_arena_tiltakskode_idx
    on arena_deltaker (arena_tiltakskode);

create trigger set_timestamp
    before update
    on arena_deltaker
    for each row
execute procedure trigger_set_timestamp();
