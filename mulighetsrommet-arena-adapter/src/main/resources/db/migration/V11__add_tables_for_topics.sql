create table tiltakstype
(
    id            uuid primary key,
    tiltakskode   text unique not null,
    navn          text        not null,
    innsatsgruppe integer     not null,
    fra_dato      timestamp,
    til_dato      timestamp
);

create table sak
(
    sak_id     integer primary key,
    lopenummer integer not null,
    aar        integer not null,
    unique (lopenummer, aar)
);

create table tiltaksgjennomforing
(
    id                      uuid primary key,
    tiltaksgjennomforing_id integer unique not null,
    sak_id                  integer        not null references sak (sak_id) on delete cascade,
    tiltakskode             text           not null references tiltakstype (tiltakskode) on delete cascade,
    arrangor_id             integer,
    navn                    text,
    fra_dato                timestamp,
    til_dato                timestamp,
    apent_for_innsok        boolean        not null,
    antall_plasser          integer
);

create table deltaker
(
    id                      uuid primary key,
    tiltaksdeltaker_id      integer not null unique,
    tiltaksgjennomforing_id integer not null references tiltaksgjennomforing (tiltaksgjennomforing_id),
    person_id               integer not null,
    fra_dato                timestamp,
    til_dato                timestamp,
    status                  text    not null
);

create table arena_entity_mapping
(
    arena_table             text not null,
    arena_id                text not null,
    tiltakstype_id          uuid,
    tiltaksgjennomforing_id uuid,
    deltaker_id             uuid
);
