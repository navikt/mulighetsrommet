drop view if exists view_deltaker;

alter table deltaker_deltakelsesmengde
    add column dager_per_uke real;

alter table deltaker_deltakelsesmengde
    alter column deltakelsesprosent type real using deltakelsesprosent::real;
