drop table arrangor_ansatt_rolle;
drop table arrangor_ansatt;
drop type arrangor_rolle;

create type altinn_ressurs as enum ('TILTAK_ARRANGOR_REFUSJON');

CREATE TABLE altinn_person_rettighet
(
    norsk_ident         text NOT NULL,
    organisasjonsnummer text NOT NULL,
    rettighet           altinn_ressurs NOT NULL,
    expiry              timestamp not null,
    primary key (norsk_ident, organisasjonsnummer)
);
