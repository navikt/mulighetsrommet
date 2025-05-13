drop view if exists avtale_admin_dto_view;

alter table avtale
    alter opsjon_maks_varighet type date;

alter table deltaker
    rename registrert_tidspunkt to registrert_dato;

alter table deltaker
    alter registrert_dato type date;

alter table gjennomforing
    alter tilgjengelig_for_arrangor_fra_og_med_dato type date;

alter table totrinnskontroll
    alter besluttet_tidspunkt drop default;
