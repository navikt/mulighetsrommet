drop view if exists avtale_admin_dto_view;
drop view if exists tiltaksgjennomforing_admin_dto_view;

drop table if exists tiltakstype_nus_kodeverk cascade;
drop table if exists nus_kodeverk cascade;

alter table avtale
    drop column if exists nusdata;

alter table tiltaksgjennomforing
    drop column if exists nusdata;
