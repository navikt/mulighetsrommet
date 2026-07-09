drop view if exists tiltaksgjennomforing_admin_dto_view;
alter table tiltaksgjennomforing
    drop column fremmote_tidspunkt,
    drop column fremmote_sted;
