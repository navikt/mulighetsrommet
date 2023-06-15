create table if not exists tiltaksgjennomforing_kontaktperson
(
    tiltaksgjennomforing_id uuid not null
        constraint fk_tiltaksgjennomforing references tiltaksgjennomforing (id) on delete cascade,
    enheter                text[] not null,
    kontaktperson_nav_ident text not null
);

alter table tiltaksgjennomforing_kontaktperson
    add constraint unique_kontaktperson unique (tiltaksgjennomforing_id, kontaktperson_nav_ident);

create type nav_ansatt_rolle as enum ('TEAM_MULIGHETSROMMET', 'BETABRUKER', 'KONTAKTPERSON', 'UKJENT');

alter table nav_ansatt
    add column rolle nav_ansatt_rolle not null default 'UKJENT';
