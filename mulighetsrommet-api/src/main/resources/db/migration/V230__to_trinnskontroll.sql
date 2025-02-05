drop view if exists tilsagn_admin_dto_view;
drop view if exists tilsagn_arrangorflate_view;

alter table tilsagn
    drop column status_endret_av,
    drop column status_besluttet_av,
    drop column status_endret_tidspunkt,
    drop column status_aarsaker,
    drop column status_forklaring;

create type to_trinnskontroll_handling as enum (
    'FORESLA_OPPRETT',
    'FORESLA_ANNULLER',
    'GODKJENN',
    'AVVIS'
);

create table to_trinnskontroll_handling_log
(
    id                  serial primary key,
    entity_id           uuid not null,
    handling            to_trinnskontroll_handling,
    created_at          timestamp not null default now(),
    opprettet_av        text not null,
    aarsaker            text[],
    forklaring          text
);
create index idx_to_trinnskontroll_handling_log_entity_id on to_trinnskontroll_handling_log(entity_id);
