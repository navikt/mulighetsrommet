drop view if exists view_tilskudd_behandling;

alter table tilskudd
    add column belop int;

update tilskudd set belop = 100 where vedtak_resultat = 'INNVILGELSE';

alter table tilskudd
    alter column belop set not null;


alter table tilskudd_behandling
    add column kommentar_intern text;
