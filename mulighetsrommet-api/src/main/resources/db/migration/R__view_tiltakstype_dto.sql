-- ${flyway:timestamp}

drop view if exists view_tiltakstype_dto;

create view view_tiltakstype_dto as
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
