drop view if exists view_tilskudd_vedtak;

alter table tilskudd_vedtak
    rename column kid to betalingsreferanse_kid;

alter table tilskudd_vedtak
    add column betalingsreferanse_melding text;
