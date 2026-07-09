alter table tiltaksgjennomforing
    drop constraint tiltaksgjennomforing_avtale_id_fkey;

alter table tiltaksgjennomforing
    add constraint tiltaksgjennomforing_avtale_id_fkey foreign key (avtale_id) references avtale (id) on delete set null;
