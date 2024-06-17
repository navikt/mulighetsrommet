drop table if exists tiltakstype_nus_kodeverk cascade;
drop table if exists nus_kodeverk cascade;

alter table avtale
    drop column if exists nusdata;

alter table tiltaksgjennomforing
    drop column if exists nusdata;
