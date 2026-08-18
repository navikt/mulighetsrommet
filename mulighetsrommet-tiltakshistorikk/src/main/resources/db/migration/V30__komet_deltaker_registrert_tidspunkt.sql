alter table komet_deltaker
    rename column registrert_dato to opprettet_tidspunkt;

alter table komet_deltaker
    rename column endret_dato to oppdatert_tidspunkt;

alter table komet_deltaker
    rename column status_opprettet_dato to status_opprettet_tidspunkt;
