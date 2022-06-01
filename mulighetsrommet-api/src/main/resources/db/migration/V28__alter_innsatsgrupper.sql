alter table innsatsgruppe
    rename column tittel to navn;
alter table innsatsgruppe
    drop column beskrivelse;
