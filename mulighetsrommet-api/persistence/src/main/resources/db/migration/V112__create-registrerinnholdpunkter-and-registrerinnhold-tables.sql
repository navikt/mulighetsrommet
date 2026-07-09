alter table tiltakstype add column deltaker_registrering_ledetekst text;
alter table tiltakstype add constraint tiltakstype_tiltakskode_unik unique (tiltakskode);

create table deltaker_registrering_innholdselement
(
    innholdskode text primary key,
    tekst text not null,
    created_at timestamp default now() not null,
    updated_at timestamp default now() not null
);

create table tiltakstype_deltaker_registrering_innholdselement(
    innholdskode text not null references deltaker_registrering_innholdselement(innholdskode),
    tiltakskode text not null references tiltakstype(tiltakskode),
    created_at timestamp default now() not null,
    updated_at timestamp default now() not null,
    primary key (innholdskode, tiltakskode)
);
