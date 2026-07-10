drop view if exists view_avtale;
drop view if exists view_arrangorflate_tiltak;
drop view if exists view_gjennomforing;

alter table prismodell
    rename column med_deltakere to tilsagn_per_deltaker;
