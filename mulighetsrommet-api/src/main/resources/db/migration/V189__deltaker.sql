alter table deltaker
    drop status,
    alter column endret_tidspunkt set not null,
    alter column status_type set not null,
    alter column status_opprettet_tidspunkt set not null;

drop type deltakerstatus;
