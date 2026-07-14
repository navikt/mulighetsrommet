drop view if exists view_individuell_gjennomforing;

create table individuell_gjennomforing
(
    id                     uuid primary key,
    navn                   text        not null,
    tiltakstype_id         uuid references tiltakstype (id) on delete set null,
    sted_for_gjennomforing text,
    arrangor_id            uuid references arrangor (id) on delete set null,
    faneinnhold            jsonb,
    beskrivelse            text,
    created_at             timestamptz not null default now(),
    updated_at             timestamptz not null default now()
);

create table individuell_gjennomforing_administrator
(
    gjennomforing_id uuid not null references individuell_gjennomforing (id) on delete cascade,
    nav_ident        text not null,
    primary key (gjennomforing_id, nav_ident)
);

create table individuell_gjennomforing_nav_enhet
(
    gjennomforing_id uuid not null references individuell_gjennomforing (id) on delete cascade,
    enhetsnummer     text not null,
    primary key (gjennomforing_id, enhetsnummer)
);

create table individuell_gjennomforing_kontaktperson
(
    gjennomforing_id        uuid not null references individuell_gjennomforing (id) on delete cascade,
    kontaktperson_nav_ident text not null,
    beskrivelse             text,
    primary key (gjennomforing_id, kontaktperson_nav_ident)
);

create table individuell_gjennomforing_arrangor_kontaktperson
(
    gjennomforing_id          uuid not null references individuell_gjennomforing (id) on delete cascade,
    arrangor_kontaktperson_id uuid not null references arrangor_kontaktperson (id) on delete cascade,
    primary key (gjennomforing_id, arrangor_kontaktperson_id)
);
