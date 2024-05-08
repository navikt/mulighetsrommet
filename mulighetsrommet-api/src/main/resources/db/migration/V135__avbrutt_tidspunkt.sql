drop view if exists tiltaksgjennomforing_admin_dto_view;
drop view if exists avtale_admin_dto_view;

alter table tiltaksgjennomforing add column avbrutt_tidspunkt timestamp;

update tiltaksgjennomforing set avbrutt_tidspunkt = start_dato + interval '0' day
    where avslutningsstatus = 'AVBRUTT';
update tiltaksgjennomforing set avbrutt_tidspunkt = start_dato - interval '1' day
    where avslutningsstatus = 'AVLYST';

alter table tiltaksgjennomforing drop column avslutningsstatus;


alter table avtale add column avbrutt_tidspunkt timestamp;

update avtale set avbrutt_tidspunkt = start_dato + interval '0' day
    where avslutningsstatus = 'AVBRUTT';
update avtale set avbrutt_tidspunkt = start_dato - interval '1' day
    where avslutningsstatus = 'AVLYST';

alter table avtale drop column avslutningsstatus;
