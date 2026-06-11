drop view if exists tilsagn_admin_dto_view;
drop view if exists tilsagn_arrangorflate_view;

alter table tilsagn
    drop column status,
    drop column status_besluttet_av,
    drop column status_endret_av,
    drop column status_endret_tidspunkt,
    drop column status_aarsaker,
    drop column status_forklaring;

create type totrinnskontroll_type as enum (
    'OPPRETT',
    'ANNULLER'
);

create table totrinnskontroll
(
    id                  uuid primary key,
    entity_id           uuid not null,
    type                totrinnskontroll_type,
    behandlet_tidspunkt timestamp not null default now(),
    behandlet_av        text not null,
    besluttet_av        text,
    besluttelse         besluttelse,
    besluttet_tidspunkt timestamp,
    aarsaker            text[] not null,
    forklaring          text
);
create index idx_totrinnskontroll_entity_id on totrinnskontroll(entity_id);

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

create function tilsagn_status(opprett_besluttelse besluttelse, annullering_behandlet_av text, annullering_besluttelse besluttelse)
returns varchar
language plpgsql
as
$$
begin
   return case
       when opprett_besluttelse is null then 'TIL_GODKJENNING'
       when annullering_besluttelse = 'AVVIST' then 'GODKJENT'
       when annullering_besluttelse = 'GODKJENT' then 'ANNULLERT'
       when annullering_behandlet_av is not null then 'TIL_ANNULLERING'
       when opprett_besluttelse = 'AVVIST' then 'RETURNERT'
       when opprett_besluttelse = 'GODKJENT' then 'GODKJENT'
   end;
end;
$$;
