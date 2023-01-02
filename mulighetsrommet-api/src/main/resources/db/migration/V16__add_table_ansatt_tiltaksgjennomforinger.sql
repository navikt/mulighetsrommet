create table ansatt_tiltaksgjennomforing
(
    id                      uuid      default gen_random_uuid() primary key,
    navident                text                    not null,
    tiltaksgjennomforing_id uuid                    not null
        constraint fk_tiltaksgjennomforing references tiltaksgjennomforing (id) on delete cascade,
    created_at              timestamp default now() not null,
    updated_at              timestamp default now() not null
);

create index ansatt_tiltaksgjennomforing_oppslag_navident_idx
    on ansatt_tiltaksgjennomforing (navident);
