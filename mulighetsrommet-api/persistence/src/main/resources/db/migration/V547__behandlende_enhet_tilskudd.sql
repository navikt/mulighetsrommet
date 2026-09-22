drop view if exists view_tilskudd_behandling;

alter table tilskudd_behandling
    add column behandlende_enhet text;
