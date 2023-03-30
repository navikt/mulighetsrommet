create type avtaleopphav as enum ('ARENA', 'MR_ADMIN_FLATE');

alter table avtale
    alter column avtalenummer drop not null,
    add column opphav         avtaleopphav not null default 'ARENA',
    add column antall_plasser int,
    add column url            text;

create table avtale_ansvarlig
(
    navident   text                    not null,
    avtale_id  uuid                    not null
        constraint fk_avtale references avtale (id) on delete cascade,
    created_at timestamp default now() not null,
    updated_at timestamp default now() not null,
    primary key (avtale_id, navident)
);


