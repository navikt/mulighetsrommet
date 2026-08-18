alter table komet_deltaker
    rename column registrert_dato to registrert_tidspunkt;

alter table komet_deltaker
    rename column endret_dato to endret_tidspunkt;

alter table komet_deltaker
    rename column status_opprettet_dato to status_opprettet_tidspunkt;
