drop view if exists avtale_admin_dto_view;

alter table avtale_opsjon_logg
    drop column registrert_dato,
    alter forrige_sluttdato set not null;

