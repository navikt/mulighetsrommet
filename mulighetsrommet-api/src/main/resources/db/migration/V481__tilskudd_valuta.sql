drop view if exists view_tilskudd_behandling;

alter table tilskudd
    add column valuta currency;

update tilskudd set valuta = soknad_valuta;

alter table tilskudd
    alter column belop drop not null;

alter table tilskudd
    drop column soknad_valuta;
