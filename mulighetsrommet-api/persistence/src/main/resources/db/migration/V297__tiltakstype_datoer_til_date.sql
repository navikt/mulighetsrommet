drop view if exists tiltakstype_admin_dto_view;

alter table tiltakstype
    alter start_dato type date,
    alter slutt_dato type date;
