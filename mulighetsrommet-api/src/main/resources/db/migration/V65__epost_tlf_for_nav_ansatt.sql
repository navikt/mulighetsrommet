alter table nav_ansatt
    add column mobilnummer text not null default '',
    add column epost text not null default '';
