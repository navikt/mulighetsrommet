alter table avtale_utdanningsprogram
    add primary key (avtale_id, utdanningsprogram_id, utdanning_id);

alter table tiltaksgjennomforing_utdanningsprogram
    add primary key (tiltaksgjennomforing_id, utdanningsprogram_id, utdanning_id);
