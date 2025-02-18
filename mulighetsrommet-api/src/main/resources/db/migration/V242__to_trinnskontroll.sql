drop view if exists tilsagn_admin_dto_view;
drop view if exists tilsagn_arrangorflate_view;

alter table tilsagn
    drop column status_endret_av,
    drop column status_besluttet_av,
    drop column status_endret_tidspunkt,
    drop column status_aarsaker,
    drop column status_forklaring;

create type to_trinnskontroll_type as enum (
    'OPPRETT',
    'ANNULLER'
);

create table to_trinnskontroll
(
    id                  serial primary key,
    entity_id           uuid not null,
    type                to_trinnskontroll_type,
    opprettet_tidspunkt timestamp not null default now(),
    opprettet_av        text not null,
    besluttet_av        text,
    besluttelse         besluttelse,
    besluttet_tidspunkt timestamp,
    aarsaker            text[] not null,
    forklaring          text,
    UNIQUE (entity_id, type)
);
create index idx_to_trinnskontroll_entity_id on to_trinnskontroll(entity_id);

alter table delutbetaling
    drop column opprettet_av,
    drop column besluttet_av,
    drop column besluttelse,
    drop column besluttet_tidspunkt,
    drop column aarsaker,
    drop column forklaring,

    drop constraint delutbetaling_pkey,
    add column id uuid default gen_random_uuid(),
    add primary key (id),
    add constraint delutbetaling_tilsagn_id_utbetaling_id_unique unique (tilsagn_id, utbetaling_id);
