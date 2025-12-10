drop view if exists view_gjennomforing;
drop view if exists view_veilederflate_tiltak;

alter table gjennomforing
    drop column sted_for_gjennomforing;
