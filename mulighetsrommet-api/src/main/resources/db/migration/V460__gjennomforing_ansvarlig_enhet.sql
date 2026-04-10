drop view if exists view_gjennomforing;

alter table gjennomforing
    rename kostnadssted to ansvarlig_enhet;
