alter table avtale
drop
column avtalestatus,
    add avslutningsstatus avslutningsstatus not null default 'IKKE_AVSLUTTET';
