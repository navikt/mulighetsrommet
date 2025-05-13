drop view if exists avtale_admin_dto_view;

alter table avtale
    alter opsjon_maks_varighet type date;
