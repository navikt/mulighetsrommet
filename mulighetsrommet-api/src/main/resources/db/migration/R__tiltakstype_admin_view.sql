drop view if exists tiltakstype_admin_dto_view;

create view tiltakstype_admin_dto_view as
select
    tiltakstype.id,
    tiltakstype.navn,
    tiltakstype.tiltakskode,
    tiltakstype.arena_kode,
    tiltakstype.registrert_dato_i_arena,
    tiltakstype.sist_endret_dato_i_arena,
    tiltakstype.start_dato,
    tiltakstype.slutt_dato,
    tiltakstype.sanity_id,
    tiltakstype.rett_paa_tiltakspenger,
    case
        when now() > slutt_dato then 'AVSLUTTET'
        when now() >= start_dato then 'AKTIV'
        else 'PLANLAGT'
    end as status,
    coalesce(
        jsonb_agg(
            jsonb_build_object(
                'personopplysning', tiltakstype_personopplysning.personopplysning,
                'frekvens', tiltakstype_personopplysning.frekvens
            )
        )
        filter (where tiltakstype_personopplysning.tiltakskode is not null), '[]'
    ) as personopplysninger
from tiltakstype
    left join tiltakstype_personopplysning on tiltakstype_personopplysning.tiltakskode = tiltakstype.tiltakskode
group by tiltakstype.id
