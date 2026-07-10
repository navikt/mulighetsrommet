alter table tiltakstype
    add column beskrivelse text,
    add column faneinnhold jsonb;

create table tiltakstype_kombinasjon
(
    tiltakstype_id    uuid not null references tiltakstype (id) on delete cascade,
    kombineres_med_id uuid not null references tiltakstype (id) on delete cascade,
    primary key (tiltakstype_id, kombineres_med_id)
);

create table redaksjonelt_innhold_lenke
(
    id          uuid primary key,
    url         text not null,
    navn        text,
    beskrivelse text
);

create table tiltakstype_faglenke
(
    tiltakstype_id uuid    not null references tiltakstype (id) on delete cascade,
    lenke_id       uuid    not null references redaksjonelt_innhold_lenke (id),
    sort_order     integer not null,
    primary key (tiltakstype_id, lenke_id)
);
