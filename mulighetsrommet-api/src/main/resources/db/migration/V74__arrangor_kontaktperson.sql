alter table tiltaksgjennomforing add column arrangor_kontaktperson_id uuid references virksomhet_kontaktperson (id);
alter table tiltaksgjennomforing rename column virksomhetsnummer to arrangor_organisasjonsnummer;
