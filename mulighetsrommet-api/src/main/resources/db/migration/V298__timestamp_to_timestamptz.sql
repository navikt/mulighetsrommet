alter table altinn_person_rettighet
    alter expiry type timestamptz using expiry at time zone 'UTC';

alter table totrinnskontroll
    alter behandlet_tidspunkt drop default,
    alter behandlet_tidspunkt type timestamptz using behandlet_tidspunkt at time zone 'Europe/Oslo',
    alter besluttet_tidspunkt type timestamptz using besluttet_tidspunkt at time zone 'Europe/Oslo';

alter table nav_ansatt
    alter skal_slettes_dato type date;
