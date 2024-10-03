drop view if exists tilsagn_admin_dto_view;

alter table tilsagn drop column beregning;
alter table tilsagn add column belop integer not null;
