alter type deltakerstatus add value 'PABEGYNT_REGISTRERING';

create table deltaker
(
    id                      uuid primary key,
    tiltaksgjennomforing_id uuid references tiltaksgjennomforing (id) not null,
    norsk_ident             text                                      not null,
    status                  deltakerstatus                            not null,
    start_dato              date,
    slutt_dato              date,
    registrert_dato         timestamp                                 not null,
    unique (tiltaksgjennomforing_id, norsk_ident)
);

create index deltaker_tiltaksgjennomforing_id_status_idx on deltaker (tiltaksgjennomforing_id, status);
