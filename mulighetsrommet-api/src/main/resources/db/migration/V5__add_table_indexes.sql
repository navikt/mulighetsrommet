alter table tiltaksgjennomforing
    add constraint unique_sak_id unique (sak_id);

create index tiltaksgjennomforing_id_status
    on deltaker (tiltaksgjennomforing_id, status);
