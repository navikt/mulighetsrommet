alter table altinn_person_rettighet
    alter expiry type timestamptz using expiry at time zone 'UTC';

alter table totrinnskontroll
    alter behandlet_tidspunkt drop default;
