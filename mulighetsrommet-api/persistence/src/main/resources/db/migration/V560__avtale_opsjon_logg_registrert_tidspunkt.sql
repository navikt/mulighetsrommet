drop view if exists view_avtale;

alter table avtale_opsjon_logg
    rename column created_at to registrert_tidspunkt;

alter table avtale_opsjon_logg
    alter registrert_tidspunkt drop default,
    alter registrert_tidspunkt type timestamptz using registrert_tidspunkt at time zone 'Europe/Oslo';
