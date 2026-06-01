create or replace view view_gjennomforing_opplaring_kategorisering as
select
    gak.gjennomforing_id,
    coalesce(gak.norskprove, false) as norskprove,
    coalesce(
            (select jsonb_agg(elem::text) from unnest(gak.innhold_elementer) as elem),
            '[]'::jsonb
                        ) as innhold_elementer,
    (select jsonb_strip_nulls(
                                jsonb_build_object(
                                        'id', okk.id,
                                        'navn', okk.navn,
                                        'kode', okk.kode,
                                        'aktiv', okk.aktiv
                                )
                        )
                 from opplaring_kategorisering_kurstype okk
                 where okk.id = gak.kurstype_id) as kurstype,
    (select jsonb_strip_nulls(
                               jsonb_build_object(
                                       'id', okb.id,
                                       'navn', okb.navn,
                                       'kode', okb.kode
                               )
                       )
                from opplaring_kategorisering_bransje okb
                where okb.id = gak.bransje_id) as bransje,
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
                      join gjennomforing_amo_kategorisering_forerkort gokf on gokf.forerkort_id = okf.id
             where gokf.gjennomforing_id = gak.gjennomforing_id),
            '[]'::jsonb) as forerkort,
    coalesce((select jsonb_strip_nulls(
                             jsonb_agg(
                                     jsonb_build_object(
                                             'label', s.label,
                                             'konseptId', s.konsept_id
                                     )
                             ))
              from amo_sertifisering s
                       join gjennomforing_amo_kategorisering_sertifisering gaks
                            on gaks.konsept_id = s.konsept_id
              where gaks.gjennomforing_id = gak.gjennomforing_id),
             '[]'::jsonb) as sertifiseringer
from gjennomforing_amo_kategorisering gak
