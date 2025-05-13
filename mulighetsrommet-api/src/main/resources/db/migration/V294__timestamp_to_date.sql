drop view if exists avtale_admin_dto_view;
drop view if exists gjennomforing_admin_dto_view;
drop view if exists utbetaling_dto_view;

alter table avtale
    alter opsjon_maks_varighet type date;

alter table deltaker
    rename registrert_tidspunkt to registrert_dato;

alter table deltaker
    alter registrert_dato type date;

alter table gjennomforing
    rename tilgjengelig_for_arrangor_fra_og_med_dato to tilgjengelig_for_arrangor_dato;

alter table gjennomforing
    alter tilgjengelig_for_arrangor_dato type date;

alter table totrinnskontroll
    alter besluttet_tidspunkt drop default;

alter table utbetaling
    alter frist_for_godkjenning type date;

update utbetaling
set frist_for_godkjenning = upper(periode) + make_interval(months := 2) - make_interval(days := 1);

alter table avtale_opsjon_logg
    alter registrert_dato drop default;

alter table avtale_opsjon_logg
    alter registrert_dato type date,
    alter sluttdato type date,
    alter forrige_sluttdato type date;
