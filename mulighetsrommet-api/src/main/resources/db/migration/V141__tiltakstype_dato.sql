drop view if exists tiltakstype_admin_dto_view;

alter table tiltakstype
    rename fra_dato to start_dato;
alter table tiltakstype
    rename til_dato to slutt_dato;
alter table tiltakstype
    alter slutt_dato drop not null,
    drop sist_endret_dato_i_arena,
    drop registrert_dato_i_arena;

alter table tiltakstype
    add innsatsgrupper innsatsgruppe[];
update tiltakstype
set innsatsgrupper = array ['STANDARD_INNSATS', 'SITUASJONSBESTEMT_INNSATS', 'SPESIELT_TILPASSET_INNSATS', 'VARIG_TILPASSET_INNSATS']::innsatsgruppe[]
where innsatsgruppe = 'STANDARD_INNSATS'::innsatsgruppe;
update tiltakstype
set innsatsgrupper = array ['SITUASJONSBESTEMT_INNSATS', 'SPESIELT_TILPASSET_INNSATS', 'VARIG_TILPASSET_INNSATS']::innsatsgruppe[]
where innsatsgruppe = 'SITUASJONSBESTEMT_INNSATS'::innsatsgruppe;
update tiltakstype
set innsatsgrupper = array ['SPESIELT_TILPASSET_INNSATS', 'VARIG_TILPASSET_INNSATS']::innsatsgruppe[]
where innsatsgruppe = 'SPESIELT_TILPASSET_INNSATS'::innsatsgruppe;
update tiltakstype
set innsatsgrupper = array ['VARIG_TILPASSET_INNSATS']::innsatsgruppe[]
where innsatsgruppe = 'VARIG_TILPASSET_INNSATS'::innsatsgruppe;

alter table tiltakstype
    drop innsatsgruppe;
