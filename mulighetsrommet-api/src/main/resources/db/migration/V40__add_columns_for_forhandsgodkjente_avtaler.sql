alter table avtale
    alter column avtalenummer drop not null,
    add column antall_plasser int,
    add column url text;

