drop view if exists avtale_admin_dto_view;

alter table avtale
    alter opsjon_maks_varighet type date;

alter table deltaker
    rename registrert_tidspunkt to registrert_dato;

alter table deltaker
    alter registrert_dato type date;
