create table tiltaksgjennomforing_enhet
(
    tiltaksgjennomforing_id uuid      not null constraint fk_tiltaksgjennomforing references tiltaksgjennomforing (id) on delete cascade,
    enhetsnummer            integer   not null constraint fk_enhetsnummer references enhet (enhetsnummer) on delete cascade,
    created_at              timestamp default now() not null,
    updated_at              timestamp default now() not null,
    primary key (tiltaksgjennomforing_id, enhetsnummer)
);
