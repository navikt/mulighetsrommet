drop view if exists view_tilskudd_behandling;

alter table tilskudd_vedtak
    add column belop int;

update tilskudd_vedtak set belop = 100 where vedtak_resultat = 'INNVILGELSE';

alter table tilskudd_vedtak
    alter column belop set not null;


alter table tilskudd_behandling
    add column kommentar_intern text;
