drop view if exists view_tiltak_dokument;

alter table tiltak_dokument rename to tiltak_beskrivelse;
alter table tiltak_dokument_administrator rename to tiltak_beskrivelse_administrator;
alter table tiltak_dokument_nav_enhet rename to tiltak_beskrivelse_nav_enhet;
alter table tiltak_dokument_kontaktperson rename to tiltak_beskrivelse_kontaktperson;
alter table tiltak_dokument_arrangor_kontaktperson rename to tiltak_beskrivelse_arrangor_kontaktperson;

alter table tiltak_beskrivelse_administrator rename column tiltak_dokument_id to tiltak_beskrivelse_id;
alter table tiltak_beskrivelse_nav_enhet rename column tiltak_dokument_id to tiltak_beskrivelse_id;
alter table tiltak_beskrivelse_kontaktperson rename column tiltak_dokument_id to tiltak_beskrivelse_id;
alter table tiltak_beskrivelse_arrangor_kontaktperson rename column tiltak_dokument_id to tiltak_beskrivelse_id;

alter table del_med_bruker rename column tiltak_dokument_id to tiltak_beskrivelse_id;
alter table del_med_bruker rename constraint fk_del_med_bruker_tiltak_dokument to fk_del_med_bruker_tiltak_beskrivelse;

-- endringshistorikk_type has an "on update cascade" foreign key from endringshistorikk.document_class,
-- so updating the value here rewrites all referencing rows automatically.
update endringshistorikk_type set value = 'TILTAK_BESKRIVELSE' where value = 'TILTAK_DOKUMENT';
