create type gruppetiltak_tiltakskode as enum (
    'AVKLARING',
    'OPPFOLGING',
    'GRUPPE_ARBEIDSMARKEDSOPPLAERING',
    'JOBBKLUBB',
    'DIGITALT_OPPFOLGINGSTILTAK',
    'ARBEIDSFORBEREDENDE_TRENING',
    'GRUPPE_FAG_OG_YRKESOPPLAERING',
    'ARBEIDSRETTET_REHABILITERING',
    'VARIG_TILRETTELAGT_ARBEID_SKJERMET');

create type gruppetiltak_status as enum (
    'PLANLAGT',
    'GJENNOMFORES',
    'AVSLUTTET',
    'AVBRUTT',
    'AVLYST');

create type gruppetiltak_oppstartstype as enum (
    'LOPENDE',
    'FELLES');


create table gruppetiltak
(
    id                           uuid                       not null primary key,
    navn                         text                       not null,
    tiltakskode                  gruppetiltak_tiltakskode   not null,
    arrangor_organisasjonsnummer text                       not null,
    start_dato                   date                       not null,
    slutt_dato                   date,
    status                       gruppetiltak_status        not null,
    oppstart                     gruppetiltak_oppstartstype not null
);
