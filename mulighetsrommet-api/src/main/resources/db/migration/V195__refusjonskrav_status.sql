drop view if exists refusjonskrav_aft_view;
drop view if exists refusjonskrav_admin_dto_view;

alter table refusjonskrav
    add column godkjent_av_arrangor_tidspunkt timestamp,
    drop column status;
