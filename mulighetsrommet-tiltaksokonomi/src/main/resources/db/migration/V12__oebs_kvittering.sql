alter table bestilling
    add column feil_melding text,
    add column feil_kode text;

alter table faktura
    add column feil_melding text,
    add column feil_kode text;

create table oebs_kvittering(
    id          serial not null primary key,
    json        jsonb
)
