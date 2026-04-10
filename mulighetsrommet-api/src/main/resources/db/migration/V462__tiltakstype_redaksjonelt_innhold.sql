drop view if exists view_tiltakstype_dto;

alter table tiltakstype
    add column if not exists beskrivelse           text,
    add column if not exists faneinnhold           jsonb,
    add column if not exists regelverklenker       jsonb;

create table if not exists tiltakstype_kombinasjon
(
    tiltakstype_id    uuid not null references tiltakstype (id) on delete cascade,
    kombineres_med_id uuid not null references tiltakstype (id) on delete cascade,
    primary key (tiltakstype_id, kombineres_med_id)
);
