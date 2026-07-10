drop view if exists tilsagn_arrangorflate_view;
drop view if exists tilsagn_admin_dto_view;

alter table tilsagn
    drop arrangor_id;

alter table tilsagn
    add column bestilling_status text;

alter table delutbetaling
    add column faktura_status text;
