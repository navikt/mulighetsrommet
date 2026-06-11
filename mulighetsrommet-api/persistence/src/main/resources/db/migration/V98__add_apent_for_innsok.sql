alter table tiltaksgjennomforing
    add apent_for_innsok boolean;

update tiltaksgjennomforing
set apent_for_innsok = tilgjengelighet != 'STENGT';

alter table tiltaksgjennomforing
    alter column apent_for_innsok set not null;
