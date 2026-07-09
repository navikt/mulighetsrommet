drop view if exists refusjonskrav_aft_view;
drop view if exists refusjonskrav_admin_dto_view;

alter table refusjonskrav_deltakelse_periode
    rename column prosent_stilling to deltakelsesprosent;

alter table deltaker
    rename column stillingsprosent to deltakelsesprosent;
