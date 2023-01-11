alter table tiltakstype
    add column rett_paa_tiltakspenger bool,
    alter column fra_dato set not null,
    alter column til_dato set not null;
