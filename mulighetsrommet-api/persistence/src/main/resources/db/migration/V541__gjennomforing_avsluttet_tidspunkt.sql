drop view if exists view_gjennomforing;
drop view if exists view_gjennomforing_kompakt;

alter table gjennomforing
    drop avsluttet_tidspunkt;
