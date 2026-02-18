drop view if exists view_tilsagn;

alter table tilsagn
  add column journalpost_id text;
