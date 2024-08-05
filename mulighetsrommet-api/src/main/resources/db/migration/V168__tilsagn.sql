create table tilsagn
(
    id                          uuid primary key,
    tiltaksgjennomforing_id     uuid not null references tiltaksgjennomforing (id),
    created_at                  timestamp default now() not null,
    periode_start               date not null,
    periode_slutt               date not null,
    kostnadssted                text not null references nav_enhet (enhetsnummer),
    opprettet_av                text,
    besluttet_av                text,
    arrangor_id                 uuid references arrangor (id),
    annullert_tidspunkt         timestamp,
    sendt_tidspunkt             timestamp,
    lopenummer                  serial not null,
    belop                       int not null
);
