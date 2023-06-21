create table virksomhet_kontaktperson
(
    id                  uuid not null primary key,
    organisasjonsnummer text not null references virksomhet (organisasjonsnummer),
    navn                text not null,
    telefon             text,
    epost               text
);

alter table avtale add column leverandor_kontaktperson_id uuid references virksomhet_kontaktperson (id);
