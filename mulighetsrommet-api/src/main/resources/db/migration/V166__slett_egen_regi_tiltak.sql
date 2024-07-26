alter table deltaker drop constraint deltaker_tiltaksgjennomforing_id_fkey;
alter table deltaker
    add constraint deltaker_tiltaksgjennomforing_id_fkey
    foreign key (tiltaksgjennomforing_id) references tiltaksgjennomforing(id) on delete cascade;

delete from tiltaksgjennomforing
using tiltakstype
where tiltaksgjennomforing.tiltakstype_id = tiltakstype.id
  and tiltakstype.arena_kode in ('IPSUNG', 'UTVAOONAV', 'INDJOBSTOT');

alter table tiltaksgjennomforing drop column sanity_id;
