alter table tiltakstype
    alter column innsatsgruppe_id set not null,
    alter column tiltakskode set not null;

alter table tiltaksgjennomforing
    alter column tiltakskode set not null,
    alter column tiltaksnummer set not null,
    alter column arena_id set not null,
    alter column sak_id set not null;
