alter table deltaker
    add registrert_tidspunkt timestamp;

update deltaker
set registrert_tidspunkt = registrert_dato;

alter table deltaker
    alter registrert_dato drop not null,
    alter registrert_tidspunkt set not null;

