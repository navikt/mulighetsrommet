create type avslutningsstatus as enum ('IKKE_AVSLUTTET', 'AVSLUTTET', 'AVBRUTT', 'AVLYST');

alter table tiltaksgjennomforing
    add column avslutningsstatus avslutningsstatus not null default 'IKKE_AVSLUTTET';
