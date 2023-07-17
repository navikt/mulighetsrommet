create table avtale_notat
(
    id           uuid primary key,
    avtale_id    uuid      not null references avtale (id),
    opprettet_av text      not null references nav_ansatt (nav_ident) on delete cascade,
    innhold      text,
    created_at   timestamp not null default now(),
    updated_at   timestamp not null default now()
);


create table tiltaksgjennomforing_notat
(
    id                      uuid primary key,
    tiltaksgjennomforing_id uuid      not null references tiltaksgjennomforing (id),
    opprettet_av            text      not null references nav_ansatt (nav_ident) on delete cascade,
    innhold                 text      not null,
    created_at              timestamp not null default now(),
    updated_at              timestamp not null default now()
);

create index avtale_notat_opprettet_av_idx on avtale_notat (opprettet_av, avtale_id);
create index tiltaksgjennomforing_notat_opprettet_av_idx on tiltaksgjennomforing_notat (opprettet_av, tiltaksgjennomforing_id);
