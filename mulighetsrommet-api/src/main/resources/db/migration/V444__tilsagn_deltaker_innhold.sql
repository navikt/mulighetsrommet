drop view if exists view_tilsagn;
drop view if exists view_deltaker;

alter table tilsagn_deltaker
    add column innhold text;

alter table deltaker
    add column innhold jsonb;
