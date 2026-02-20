drop view if exists view_tilsagn;

alter table tilsagn
  add column journalpost_id text,
  add column journalpost_distribuering_id text;
