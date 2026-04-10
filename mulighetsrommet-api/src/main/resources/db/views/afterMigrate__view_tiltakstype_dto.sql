create or replace view view_tiltakstype_dto as
select tiltakstype.id,
       tiltakstype.navn,
       tiltakstype.tiltakskode,
       tiltakstype.arena_kode,
       tiltakstype.start_dato,
       tiltakstype.slutt_dato,
       tiltakstype.sanity_id,
       tiltakstype.innsatsgrupper,
       tiltakstype.beskrivelse,
       tiltakstype.faneinnhold,
       tiltakstype.regelverklenker,
       coalesce(
           (select jsonb_agg(to_jsonb(t2.navn) order by t2.navn)
            from tiltakstype_kombinasjon k
                join tiltakstype t2 on t2.id = k.kombineres_med_id
            where k.tiltakstype_id = tiltakstype.id),
           '[]'::jsonb
       ) as kan_kombineres_med,
       case
           when slutt_dato is not null and date(now()) > slutt_dato then 'AVSLUTTET'
           else 'AKTIV'
           end as status
from tiltakstype
group by tiltakstype.id
