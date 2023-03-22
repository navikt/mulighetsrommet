alter table tiltaksgjennomforing
    drop column if exists avtale_id;

alter table tiltaksgjennomforing
    add column avtale_id uuid references avtale(id);

create index tiltaksgjennomforing_avtale_id_idx on tiltaksgjennomforing(avtale_id);
