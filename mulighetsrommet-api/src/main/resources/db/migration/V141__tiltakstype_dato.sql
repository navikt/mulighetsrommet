drop view if exists tiltakstype_admin_dto_view;

alter table tiltakstype
    rename fra_dato to start_dato;
alter table tiltakstype
    rename til_dato to slutt_dato;
