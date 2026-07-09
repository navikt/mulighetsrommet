drop view if exists view_deltaker;
drop view if exists view_tilsagn;

alter table tilsagn_deltaker
    add column innhold_annet text;

alter table deltaker
    add column innhold_annet text;
