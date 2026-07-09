alter index enhet_idx rename to tiltaksgjennomforing_enhet_idx;
alter index tiltaksnummer_idx rename to tiltaksgjennomforing_tiltaksnummer_idx;

alter table tiltaksgjennomforing
    alter column enhet set not null;
