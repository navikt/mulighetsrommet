alter table tiltaksgjennomforing
    drop column if exists avtale_id;

alter table tiltaksgjennomforing
    add column avtale_id int;
