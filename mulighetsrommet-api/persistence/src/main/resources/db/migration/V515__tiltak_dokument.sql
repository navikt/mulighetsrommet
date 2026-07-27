drop view if exists view_tiltak_dokument;

create table tiltak_dokument
(
    id                     uuid primary key,
    navn                   text        not null,
    tiltakstype_id         uuid references tiltakstype (id) not null,
    sted_for_gjennomforing text,
    arrangor_id            uuid references arrangor (id),
    faneinnhold            jsonb,
    beskrivelse            text,
    publisert              boolean not null default false,
    sanity_id              uuid unique,
    tiltaksnummer          text,
    created_at             timestamptz not null default now(),
    updated_at             timestamptz not null default now()
);

create table tiltak_dokument_administrator
(
    tiltak_dokument_id uuid not null references tiltak_dokument (id) on delete cascade,
    nav_ident        text not null,
    primary key (tiltak_dokument_id, nav_ident)
);

create table tiltak_dokument_nav_enhet
(
    tiltak_dokument_id uuid not null references tiltak_dokument (id) on delete cascade,
    enhetsnummer     text not null,
    primary key (tiltak_dokument_id, enhetsnummer)
);

create table tiltak_dokument_kontaktperson
(
    tiltak_dokument_id uuid not null references tiltak_dokument (id) on delete cascade,
    kontaktperson_nav_ident      text not null,
    beskrivelse                  text,
    primary key (tiltak_dokument_id, kontaktperson_nav_ident)
);

create table tiltak_dokument_arrangor_kontaktperson
(
    tiltak_dokument_id          uuid not null references tiltak_dokument (id) on delete cascade,
    arrangor_kontaktperson_id uuid not null references arrangor_kontaktperson (id) on delete cascade,
    primary key (tiltak_dokument_id, arrangor_kontaktperson_id)
);
