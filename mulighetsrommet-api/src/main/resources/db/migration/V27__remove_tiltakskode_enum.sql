alter table tiltaksgjennomforing
    drop constraint fk_tiltakskode;
alter table tiltaksgjennomforing
    alter column tiltakskode TYPE text;
alter table tiltakstype
    alter column tiltakskode TYPE text;
drop type tiltakskode;
alter table tiltaksgjennomforing
    add constraint fk_tiltakskode foreign key (tiltakskode) references tiltakstype (tiltakskode);

