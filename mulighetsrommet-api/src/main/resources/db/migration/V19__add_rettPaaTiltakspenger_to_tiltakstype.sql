alter table tiltakstype
    add column rett_paa_tiltakspenger bool,
    add column fra_dato      timestamp,
    add column til_dato      timestamp;

update tiltakstype
    set fra_dato = now(),
        til_dato = now();

alter table tiltakstype
    alter column fra_dato set not null,
    alter column til_dato set not null;
