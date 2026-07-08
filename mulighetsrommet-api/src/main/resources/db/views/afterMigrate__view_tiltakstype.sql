create or replace view view_tiltakstype as
select tiltakstype.id,
       tiltakstype.navn,
       tiltakstype.tiltakskode,
       tiltakstype.arena_kode,
       tiltakstype.created_at,
       tiltakstype.updated_at,
       tiltakstype.sanity_id,
       tiltakstype.innsatsgrupper,
       tiltakstype.beskrivelse,
       tiltakstype.faneinnhold,
       tiltakstype.deltaker_registrering_ledetekst,
       coalesce(deltaker_innhold.innholdselementer, '[]'::jsonb) as deltaker_registrering_innholdselementer,
       coalesce(faglenker.lenker, '[]'::jsonb)                   as faglenker,
       coalesce(kan_kombineres_med.tiltakstyper, '[]'::jsonb)    as kan_kombineres_med
from tiltakstype
         left join lateral (select jsonb_agg(
                                           jsonb_build_object('tekst', e.tekst, 'innholdskode', te.innholdskode)
                                           order by e.tekst
                                   ) as innholdselementer
                            from tiltakstype_deltaker_registrering_innholdselement te
                                     join deltaker_registrering_innholdselement e on e.innholdskode = te.innholdskode
                            where te.tiltakstype_id = tiltakstype.id
    ) deltaker_innhold on true
         left join lateral (select jsonb_agg(
                                           jsonb_build_object(
                                                   'id', l.id,
                                                   'url', l.url,
                                                   'navn', l.navn,
                                                   'beskrivelse', l.beskrivelse
                                           ) order by tl.sort_order
                                   ) as lenker
                            from tiltakstype_faglenke tl
                                     join redaksjonelt_innhold_lenke l on l.id = tl.lenke_id
                            where tl.tiltakstype_id = tiltakstype.id
    ) faglenker on true
         left join lateral (select jsonb_agg(
                                           jsonb_build_object('id', t2.id, 'navn', t2.navn)
                                           order by t2.navn
                                   ) as tiltakstyper
                            from tiltakstype_kombinasjon k
                                     join tiltakstype t2 on t2.id = k.kombineres_med_id
                            where k.tiltakstype_id = tiltakstype.id
    ) kan_kombineres_med on true
