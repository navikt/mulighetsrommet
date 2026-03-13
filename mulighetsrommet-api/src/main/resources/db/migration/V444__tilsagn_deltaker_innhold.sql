drop view if exists view_tilsagn;

alter table tilsagn_deltaker
    add column innhold text;

alter table deltaker
    add column innhold jsonb;
