create type deltakerstatus as enum ('IKKE_AKTUELL', 'VENTER', 'DELTAR', 'AVSLUTTET');

create table innsatsgruppe
(
    id   integer not null
        primary key,
    navn text    not null
);

create table tiltakstype
(
    id               serial
        primary key,
    navn             text                    not null,
    created_at       timestamp default now() not null,
    updated_at       timestamp default now() not null,
    innsatsgruppe_id integer                 not null
        constraint tiltaksvariant_innsatsgruppe_id_fkey
            references innsatsgruppe
            on update cascade,
    sanity_id        integer,
    tiltakskode      text                    not null
        unique,
    fra_dato         timestamp,
    til_dato         timestamp,
    created_by       text,
    updated_by       text
);

create table tiltaksgjennomforing
(
    id            serial
        primary key,
    navn          text                    not null,
    tiltaksnummer integer,
    fra_dato      timestamp,
    til_dato      timestamp,
    created_at    timestamp default now() not null,
    updated_at    timestamp default now() not null,
    tiltakskode   text                    not null
        constraint fk_tiltakskode
            references tiltakstype (tiltakskode),
    arrangor_id   integer,
    arena_id      integer                 not null
        constraint arena_id_unique
            unique,
    sak_id        integer                 not null,
    sanity_id     integer,
    created_by    text,
    updated_by    text,
    aar           integer
);

create table deltaker
(
    id                      serial
        primary key,
    arena_id                integer        not null
        unique,
    tiltaksgjennomforing_id integer        not null
        constraint fk_tiltaksgjennomforing
            references tiltaksgjennomforing (arena_id),
    person_id               integer        not null,
    fra_dato                timestamp,
    til_dato                timestamp,
    status                  deltakerstatus not null
);

create view tiltaksgjennomforing_valid(id, navn, tiltaksnummer, tiltakskode, aar) as
SELECT tiltaksgjennomforing.id,
       tiltaksgjennomforing.navn,
       tiltaksgjennomforing.tiltaksnummer,
       tiltaksgjennomforing.tiltakskode,
       tiltaksgjennomforing.aar
FROM tiltaksgjennomforing
WHERE tiltaksgjennomforing.tiltaksnummer IS NOT NULL
  AND tiltaksgjennomforing.aar IS NOT NULL;
