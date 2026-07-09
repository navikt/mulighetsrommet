drop view if exists view_tilskudd_behandling;

alter table tilskudd_behandling add column vedtak_journalpost_id text;
alter table tilskudd_behandling add column vedtak_journalpost_distribuering_id text;
alter table tilskudd_behandling add column vedtak_journalfort_tidspunkt  timestamptz;
alter table tilskudd_behandling add column vedtak_distribuert_tidspunkt  timestamptz;
