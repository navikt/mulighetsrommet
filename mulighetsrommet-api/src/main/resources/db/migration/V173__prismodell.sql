drop view if exists tilsagn_admin_dto_view;

alter table tilsagn add column beregning jsonb not null;
alter table tilsagn drop column belop;
