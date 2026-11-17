alter table komet_deltaker
    alter column opprettet_tidspunkt type timestamptz
        using opprettet_tidspunkt::timestamp at time zone 'UTC';

alter table gjennomforing
    add column opprettet_tidspunkt timestamptz,
    add column oppdatert_tidspunkt timestamptz;

alter table tiltakstype
    add column opprettet_tidspunkt timestamptz,
    add column oppdatert_tidspunkt timestamptz;
