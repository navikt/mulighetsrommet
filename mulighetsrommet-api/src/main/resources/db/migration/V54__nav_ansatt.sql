create table nav_ansatt
(
    nav_ident     text primary key not null,
    fornavn       text             not null,
    etternavn     text             not null,
    hovedenhet    text             not null references nav_enhet (enhetsnummer),
    azure_id      uuid unique      not null,
    fra_ad_gruppe uuid             not null
);

create index nav_ansatt_hovedenhet_idx on nav_ansatt (hovedenhet);
