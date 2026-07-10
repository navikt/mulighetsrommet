alter table tiltakstype_deltaker_registrering_innholdselement
    add column tiltakstype_id uuid references tiltakstype (id);

update tiltakstype_deltaker_registrering_innholdselement
set tiltakstype_id = (select id
                      from tiltakstype
                      where tiltakskode = tiltakstype_deltaker_registrering_innholdselement.tiltakskode);

alter table tiltakstype_deltaker_registrering_innholdselement
    alter tiltakstype_id set not null;

alter table tiltakstype_deltaker_registrering_innholdselement
    drop constraint tiltakstype_deltaker_registrering_innholdselement_pkey;

alter table tiltakstype_deltaker_registrering_innholdselement
    drop tiltakskode;

alter table tiltakstype_deltaker_registrering_innholdselement
    add constraint tiltakstype_deltaker_registrering_innholdselement_pkey
        primary key (tiltakstype_id, innholdskode);

update tiltakstype
set tiltakskode = 'VARIG_TILRETTELAGT_ARBEID_ORDINAER',
    navn        = 'Varig tilrettelagt arbeid i ordinær virksomhet'
where tiltakskode = 'TILPASSET_JOBBSTOTTE';

update prismodell
set system_id = 'VARIG_TILRETTELAGT_ARBEID_ORDINAER'
where system_id = 'TILPASSET_JOBBSTOTTE';
