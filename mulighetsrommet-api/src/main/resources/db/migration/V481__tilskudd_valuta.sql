drop view if exists view_tilskudd_behandling;

alter table tilskudd
    add column valuta currency;

update tilskudd set valuta = 'NOK'::currency;

alter table tilskudd
    alter column belop drop not null;
