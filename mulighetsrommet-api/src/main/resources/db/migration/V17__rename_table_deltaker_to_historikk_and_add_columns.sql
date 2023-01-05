alter table deltaker
    rename to historikk;

alter table historikk
    add column beskrivelse       text,
    add column tiltakstypeid     uuid,
    add column virksomhetsnummer text;

alter table historikk
    alter column tiltaksgjennomforing_id drop not null;

