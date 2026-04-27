drop view if exists view_tiltakstype;

alter table tiltakstype
    drop column start_dato;
alter table tiltakstype
    drop column slutt_dato;
