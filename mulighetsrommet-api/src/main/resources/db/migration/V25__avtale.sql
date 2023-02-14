create type avtaletype as enum ('Avtale', 'Rammeavtale', 'Forhaandsgodkjent');

create type avtalestatus as enum ('Planlagt', 'Aktiv', 'Avsluttet', 'Avbrutt');

create table avtale
(
    id                             uuid primary key,
    navn                           text         not null,
    tiltakstype_id                 uuid         not null references tiltakstype (id),
    avtalenummer                   text unique  not null,
    leverandor_organisasjonsnummer text         not null,
    start_dato                     date         not null,
    slutt_dato                     date         not null,
    enhet                          text         not null,
    avtaletype                     avtaletype   not null,
    avtalestatus                   avtalestatus not null,
    prisbetingelser                text
);
