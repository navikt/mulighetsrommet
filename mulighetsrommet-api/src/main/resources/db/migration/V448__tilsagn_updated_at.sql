drop view if exists view_tilsagn;

alter table tilsagn
    rename type to tilsagn_type;
