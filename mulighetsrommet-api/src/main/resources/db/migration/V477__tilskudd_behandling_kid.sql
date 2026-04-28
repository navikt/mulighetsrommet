drop view if exists view_tilskudd_behandling;

alter TABLE tilskudd_vedtak
    add column kid text;
