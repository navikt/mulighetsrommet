drop view if exists tilsagn_admin_dto_view;
drop view if exists tilsagn_arrangorflate_view;

create type besluttelse as enum ('GODKJENT', 'AVVIST');
create type to_trinnskontroll_type as enum (
    'OPPRETT_TILSAGN',
    'ANNULLER_TILSAGN',
    'OPPRETT_UTBETALING'
);

create table to_trinnskontroll
(
    id                  serial primary key,
    entity_id           uuid not null,
    type                to_trinnskontroll_type,
    opprettet_tidspunkt timestamp not null default now(),
    opprettet_av        text not null,
    besluttelse         besluttelse,
    besluttet_av        text,
    besluttet_tidspunkt timestamp,
    aarsaker            text[],
    forklaring          text
);
alter table to_trinnskontroll add constraint unique_entity_type unique (entity_id, type);
alter table to_trinnskontroll add constraint chk_besluttet_opprettet_diff check (besluttet_av is null or besluttet_av <> opprettet_av);

create index idx_to_trinnskontroll_entity_id on to_trinnskontroll(entity_id);

alter table tilsagn
    drop column status_endret_av,
    drop column status_besluttet_av,
    drop column status_endret_tidspunkt,
    drop column status_aarsaker,
    drop column status_forklaring;

