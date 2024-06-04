drop view if exists tiltakstype_admin_dto_view;

create view tiltakstype_admin_dto_view as
select
    tiltakstype.id,
    tiltakstype.navn,
    tiltakstype.tiltakskode,
    tiltakstype.arena_kode,
    tiltakstype.start_dato,
    tiltakstype.slutt_dato,
    tiltakstype.sanity_id,
    tiltakstype.innsatsgrupper,
    case
        when slutt_dato is not null and date(now()) > slutt_dato then 'AVSLUTTET'
        else 'AKTIV'
    end as status
from tiltakstype
group by tiltakstype.id
