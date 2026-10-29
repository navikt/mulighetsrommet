drop view if exists view_tilskudd_vedtak;

alter table tilskudd_vedtak
    add column fakturanummer text;
