drop view if exists view_deltaker;

alter table deltaker
    alter column registrert_tidspunkt type timestamptz using registrert_tidspunkt at time zone 'Europe/Oslo',
    alter column endret_tidspunkt type timestamptz using endret_tidspunkt at time zone 'Europe/Oslo',
    alter column status_opprettet_tidspunkt type timestamptz using status_opprettet_tidspunkt at time zone 'Europe/Oslo';
