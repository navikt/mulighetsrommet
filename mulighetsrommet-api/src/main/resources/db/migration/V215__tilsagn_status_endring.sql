drop view if exists tilsagn_admin_dto_view;
drop view if exists tilsagn_arrangorflate_view;

alter type avvist_aarsak_type rename to tilsagn_status_aarsak;
alter type tilsagn_status_aarsak add value 'FEIL_REGISTRERING';
alter table tilsagn drop column besluttelse;
drop type tilsagn_besluttelse;

alter table tilsagn rename column opprettet_av to status_endret_av;
alter table tilsagn rename column besluttet_av to status_besluttet_av;
alter table tilsagn rename column avvist_aarsaker to status_aarsaker;
alter table tilsagn rename column avvist_forklaring to status_forklaring;
alter table tilsagn rename column besluttet_tidspunkt to status_endret_tidspunkt;
alter table tilsagn alter column status_endret_tidspunkt set not null;

alter table tilsagn drop column annullert_tidspunkt;

create type tilsagn_status as enum (
    'TIL_GODKJENNING',
    'GODKJENT',
    'RETURNERT',
    'TIL_ANNULLERING',
    'ANNULLERT'
);
alter table tilsagn add column status tilsagn_status not null;
