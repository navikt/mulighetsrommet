alter table deltaker
    drop opphav;

drop type deltakeropphav;

alter table deltaker
    rename column tiltaksgjennomforing_id to gjennomforing_id;

alter table deltaker
    rename column registrert_dato to registrert_tidspunkt;

create type deltaker_status_type as enum (
    'AVBRUTT',
    'AVBRUTT_UTKAST',
    'DELTAR',
    'FEILREGISTRERT',
    'FULLFORT',
    'HAR_SLUTTET',
    'IKKE_AKTUELL',
    'KLADD',
    'PABEGYNT_REGISTRERING',
    'SOKT_INN',
    'UTKAST_TIL_PAMELDING',
    'VENTELISTE',
    'VENTER_PA_OPPSTART',
    'VURDERES');

create type deltaker_status_aarsak as enum (
    'SYK',
    'FATT_JOBB',
    'TRENGER_ANNEN_STOTTE',
    'FIKK_IKKE_PLASS',
    'UTDANNING',
    'FERDIG',
    'AVLYST_KONTRAKT',
    'IKKE_MOTT',
    'FEILREGISTRERT',
    'OPPFYLLER_IKKE_KRAVENE',
    'ANNET',
    'SAMARBEIDET_MED_ARRANGOREN_ER_AVBRUTT');

alter table deltaker
    add column endret_tidspunkt           timestamp,
    add column stillingsprosent           numeric(5, 2),
    add column status_type                text,
    add column status_aarsak              text,
    add column status_opprettet_tidspunkt timestamp;
