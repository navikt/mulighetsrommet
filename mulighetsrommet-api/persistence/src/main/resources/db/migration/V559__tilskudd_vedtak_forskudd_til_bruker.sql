drop view if exists view_tilskudd_vedtak;
drop view if exists view_tilskudd_behandling;

alter table tilskudd_vedtak add column forskudd_til_bruker boolean;

update tilskudd_vedtak
    set forskudd_til_bruker = false
where forskudd_til_bruker is null;

alter table tilskudd_vedtak
    alter column forskudd_til_bruker set not null;
