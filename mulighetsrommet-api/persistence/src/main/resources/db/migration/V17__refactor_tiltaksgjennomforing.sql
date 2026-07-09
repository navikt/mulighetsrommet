alter table tiltaksgjennomforing
    rename column fra_dato to start_dato;

alter table tiltaksgjennomforing
    rename column til_dato to slutt_dato;

alter table tiltaksgjennomforing
    alter column start_dato type date,
    alter column slutt_dato type date;
