drop function if exists tilsagn_status;

alter table tilsagn add column status text not null;
alter type totrinnskontroll_type add value 'FRIGJOR';
