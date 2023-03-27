alter table avtale
drop
column avtalestatus,
    add avslutningsstatus avslutningsstatus not null default 'IKKE_AVSLUTTET';

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
