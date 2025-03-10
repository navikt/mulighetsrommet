drop view if exists tilsagn_admin_dto_view;
drop view if exists tilsagn_arrangorflate_view;

drop function if exists tilsagn_status;

alter table tilsagn add column status tilsagn_status not null;
alter type totrinnskontroll_type add value 'FRIGJOR';

alter type tilsagn_status add value 'TIL_FRIGJORING';
alter type tilsagn_status add value 'FRIGJORT';
