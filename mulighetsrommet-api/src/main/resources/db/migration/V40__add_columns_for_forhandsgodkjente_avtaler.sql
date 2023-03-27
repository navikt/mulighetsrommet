alter table avtale
    alter column avtalenummer drop not null,
    add column antall_plasser int,
    add column url text;

create table ansatt_avtale
(
    navident   text                    not null,
    avtale_id  uuid                    not null
        constraint fk_avtale references avtale (id) on delete cascade,
    created_at timestamp default now() not null,
    updated_at timestamp default now() not null,
    primary key (navident, avtale_id)
);

create index ansatt_avtale_oppslag_navident_idx
    on ansatt_avtale (navident);

create index ansatt_avtale_oppslag_avtaleid_idx
    on ansatt_avtale (avtale_id);


