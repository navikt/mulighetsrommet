alter table tiltaksgjennomforing_enhet rename to tiltaksgjennomforing_nav_enhet;

alter table avtale rename column enhet to arena_ansvarlig_enhet;
alter table avtale alter arena_ansvarlig_enhet drop not null;
alter table avtale add column nav_region text references nav_enhet (enhetsnummer);

create table avtale_nav_enhet
(
    avtale_id uuid      not null constraint fk_avtale references avtale (id) on delete cascade,
    enhetsnummer        text     not null constraint fk_enhetsnummer references nav_enhet (enhetsnummer) on delete cascade,
    created_at          timestamp default now() not null,
    updated_at          timestamp default now() not null,
    primary key (avtale_id, enhetsnummer)
);
