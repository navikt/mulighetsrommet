create table tiltaksgjennomforing_koordinator
(
    nav_ident               text                    not null,
    tiltaksgjennomforing_id uuid                    not null
        constraint fk_tiltaksgjennomforing references tiltaksgjennomforing (id) on delete cascade,
    created_at              timestamp default now() not null,
    updated_at              timestamp default now() not null,
    primary key (tiltaksgjennomforing_id, nav_ident)
);
