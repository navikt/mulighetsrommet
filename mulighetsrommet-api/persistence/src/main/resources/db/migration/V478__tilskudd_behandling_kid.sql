drop view if exists view_tilskudd_behandling;

alter TABLE tilskudd
    add column kid text;
