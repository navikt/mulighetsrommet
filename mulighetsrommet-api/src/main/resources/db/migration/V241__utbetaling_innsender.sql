drop view if exists utbetaling_aft_view;
drop view if exists utbetaling_dto_view;

alter table utbetaling add column innsender text;
