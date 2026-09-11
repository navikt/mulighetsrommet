drop view if exists view_tilsagn;

alter table tilsagn
    add column altinn_correspondence_id text;
