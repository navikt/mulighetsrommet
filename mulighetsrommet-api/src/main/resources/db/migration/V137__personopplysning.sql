create type personopplysning as enum (
    'NAVN',
    'KJONN',
    'ADRESSE',
    'TELEFONNUMMER',
    'FOLKEREGISTER_IDENTIFIKATOR',
    'FODSELSDATO',
    'BEHOV_FOR_BISTAND_FRA_NAV',
    'YTELSER_FRA_NAV',
    'BILDE',
    'EPOST',
    'BRUKERNAVN',
    'ARBEIDSERFARING_OG_VERV',
    'SERTIFIKATER_OG_KURS',
    'IP_ADRESSE',
    'UTDANNING_OG_FAGBREV',
    'PERSONLIGE_EGENSKAPER_OG_INTERESSER',
    'SPRAKKUNNSKAP',
    'ADFERD',
    'SOSIALE_FORHOLD',
    'HELSEOPPLYSNINGER',
    'RELIGION'
);

create type personopplysning_frekvens as enum (
    'ALLTID',
    'OFTE',
    'SJELDEN'
);

create table tiltakstype_personopplysning(
    id serial primary key,
    tiltakskode tiltakskode not null,
    personopplysning personopplysning not null,
    frekvens personopplysning_frekvens not null,
    unique (personopplysning, tiltakskode)
);

create table avtale_personopplysning(
    avtale_id uuid references avtale(id) on delete cascade,
    personopplysning personopplysning not null,
    primary key (avtale_id, personopplysning)
)

