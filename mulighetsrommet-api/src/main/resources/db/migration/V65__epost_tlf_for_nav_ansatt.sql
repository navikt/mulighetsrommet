alter table nav_ansatt
    add column mobilnummer text,
    add column epost text not null default '';
