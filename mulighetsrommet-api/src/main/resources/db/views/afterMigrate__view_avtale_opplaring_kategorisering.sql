create or replace view view_avtale_opplaring_kategorisering as
select
    aak.avtale_id,
    coalesce(aak.norskprove, false)              as norskprove,
    coalesce(
            (select jsonb_agg(elem::text) from unnest(aak.innhold_elementer) as elem),
            '[]'::jsonb
                        )                        as innhold_elementer,
    (select jsonb_strip_nulls(
                                jsonb_build_object(
                                        'id', okk.id,
                                        'navn', okk.navn,
                                        'kode', okk.kode,
                                        'aktiv', okk.aktiv
                                )
                        )
                 from opplaring_kategorisering_kurstype okk
                 where okk.id = aak.kurstype_id) as kurstype,
    (select jsonb_strip_nulls(
                               jsonb_build_object(
                                       'id', okb.id,
                                       'navn', okb.navn,
                                       'kode', okb.kode
                               )
                       )
                from opplaring_kategorisering_bransje okb
                where okb.id = aak.bransje_id)   as bransje,
    coalesce(
            (select jsonb_strip_nulls(
                            jsonb_agg(
                                    jsonb_build_object(
                                            'id', okf.id,
                                            'navn', okf.navn,
                                            'kode', okf.kode
                                    )
                            )
                    )
             from opplaring_kategorisering_forerkort okf
                      join avtale_amo_kategorisering_forerkort aokf on aokf.forerkort_id = okf.id
             where aokf.avtale_id = aak.avtale_id),
            '[]'::jsonb)                         as forerkort,
    coalesce((select jsonb_strip_nulls(
                             jsonb_agg(
                                     jsonb_build_object(
                                             'label', s.label,
                                             'konseptId', s.konsept_id
                                     )
                             ))
              from amo_sertifisering s
                       join avtale_amo_kategorisering_sertifisering gaks
                            on gaks.konsept_id = s.konsept_id
              where gaks.avtale_id = aak.avtale_id),
             '[]'::jsonb)                        as sertifiseringer
from avtale_amo_kategorisering aak
