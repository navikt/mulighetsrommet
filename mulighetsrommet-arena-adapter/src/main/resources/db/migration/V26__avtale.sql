create table avtale
(
    id              uuid primary key,
    aar             int       not null,
    lopenr          int       not null,
    tiltakskode     text      not null references tiltakstype (tiltakskode),
    leverandor_id   int       not null,
    navn            text      not null,
    fra_dato        timestamp not null,
    til_dato        timestamp not null,
    ansvarlig_enhet text      not null,
    rammeavtale     boolean   not null,
    status          text      not null,
    prisbetingelser text,
    unique (aar, lopenr)
);
