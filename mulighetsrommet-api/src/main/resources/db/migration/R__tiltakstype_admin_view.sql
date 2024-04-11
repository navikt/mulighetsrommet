drop view if exists tiltakstype_admin_dto_view;

create view tiltakstype_admin_dto_view as
select
    tiltakstype.id,
    tiltakstype.navn,
    tiltakstype.tiltakskode,
    tiltakstype.arena_kode,
    tiltakstype.registrert_dato_i_arena,
    tiltakstype.sist_endret_dato_i_arena,
    tiltakstype.fra_dato,
    tiltakstype.til_dato,
    tiltakstype.sanity_id,
    tiltakstype.rett_paa_tiltakspenger,
    case
        when now() > til_dato then 'Avsluttet'
        when now() >= fra_dato then 'Aktiv'
        else 'Planlagt'
    end as status,
    COALESCE(
        json_agg(
            json_build_object(
                'personopplysning', tiltakstype_personopplysning.personopplysning,
                'frekvens', tiltakstype_personopplysning.frekvens
            )
        )
        FILTER (WHERE tiltakstype_personopplysning.tiltakskode IS NOT NULL), '[]'
    ) as personopplysninger
from tiltakstype
    left join tiltakstype_personopplysning on tiltakstype_personopplysning.tiltakskode = tiltakstype.tiltakskode
group by tiltakstype.id

