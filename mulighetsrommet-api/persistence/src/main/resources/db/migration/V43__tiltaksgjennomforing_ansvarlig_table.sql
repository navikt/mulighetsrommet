create table tiltaksgjennomforing_ansvarlig
(
    navident                text      not null,
    tiltaksgjennomforing_id uuid      not null constraint fk_tiltaksgjennomforing references tiltaksgjennomforing (id) on delete cascade,
    created_at              timestamp default now() not null,
    updated_at              timestamp default now() not null,
    primary key (tiltaksgjennomforing_id, navident)
);
