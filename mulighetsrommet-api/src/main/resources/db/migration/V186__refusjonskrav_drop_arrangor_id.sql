drop view if exists refusjonskrav_admin_dto_view;

alter table refusjonskrav
    drop column arrangor_id;
