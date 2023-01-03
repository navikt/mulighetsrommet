create table ansatt_tiltaksgjennomforing
(
    navident                text                    not null,
    tiltaksgjennomforing_id uuid                    not null
        constraint fk_tiltaksgjennomforing references tiltaksgjennomforing (id) on delete cascade,
    created_at              timestamp default now() not null,
    updated_at              timestamp default now() not null,
    primary key (navident, tiltaksgjennomforing_id)
);

create index ansatt_tiltaksgjennomforing_oppslag_navident_idx
    on ansatt_tiltaksgjennomforing (navident);
