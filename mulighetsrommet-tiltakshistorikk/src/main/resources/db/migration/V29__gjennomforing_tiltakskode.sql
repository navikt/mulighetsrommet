alter table gjennomforing
    drop constraint fk_tiltakskode;

alter table gjennomforing
    add constraint fk_tiltakskode foreign key (tiltakskode) references tiltakstype (tiltakskode) on update cascade;
