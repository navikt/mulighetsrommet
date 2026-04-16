drop view if exists view_tiltakstype_dto;
drop view if exists view_tiltakstype;
drop view if exists view_datavarehus_tiltak;
drop view if exists view_gjennomforing;
drop view if exists view_gjennomforing_kompakt;
drop view if exists view_avtale;
drop view if exists view_arrangorflate_tiltak;
drop view if exists view_arrangorflate_tilsagn_kompakt;
drop view if exists view_tilsagn;
drop view if exists view_utbetaling;
drop view if exists view_veilederflate_tiltak;

alter table tiltakstype_deltaker_registrering_innholdselement
    drop constraint tiltakskode_fkey;

alter table tiltakstype_deltaker_registrering_innholdselement
    alter tiltakskode type text;

alter table tiltakstype
    alter tiltakskode type text;

alter table tiltakstype_deltaker_registrering_innholdselement
    add constraint tiltakskode_fkey
        foreign key (tiltakskode) references tiltakstype (tiltakskode);

drop type tiltakskode;
