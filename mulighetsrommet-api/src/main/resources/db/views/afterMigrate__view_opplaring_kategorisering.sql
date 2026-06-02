create or replace view view_opplaring_kategorisering as
select ok.id,
       coalesce(ok.norskprove, false)  as norskprove,
       coalesce(
               (select jsonb_agg(
                               jsonb_build_object(
                                       'id', oie.id,
                                       'navn', oie.navn,
                                       'kode', oie.kode
                               )
                       )
                from opplaring_innhold_element oie
                         inner join opplaring_kategorisering_innhold_element okie on oie.id = okie.innhold_element_id
                where okie.opplaring_kategorisering_id = ok.id),
               '[]'::jsonb)
                                       as innhold_elementer,
       (select jsonb_strip_nulls(
                       jsonb_build_object(
                               'id', okk.id,
                               'navn', okk.navn,
                               'kode', okk.kode,
                               'aktiv', okk.aktiv
                       )
               )
        from opplaring_kategorisering_kurstype okk
        where okk.id = ok.kurstype_id) as kurstype,
       (select jsonb_strip_nulls(
                       jsonb_build_object(
                               'id', okb.id,
                               'navn', okb.navn,
                               'kode', okb.kode
                       )
               )
        from opplaring_kategorisering_bransje okb
        where okb.id = ok.bransje_id)  as bransje,
       coalesce(
               (select jsonb_strip_nulls(
                               jsonb_agg(
                                       jsonb_build_object(
                                               'id', f.id,
                                               'navn', f.navn,
                                               'kode', f.kode
                                       )
                               )
                       )
                from opplaring_forerkort f
                         join opplaring_kategorisering_forerkort okf on okf.forerkort_id = f.id
                where okf.opplaring_kategorisering_id = ok.id),
               '[]'::jsonb)            as forerkort,
       coalesce((select jsonb_strip_nulls(
                                jsonb_agg(
                                        jsonb_build_object(
                                                'label', s.label,
                                                'konseptId', s.konsept_id
                                        )
                                ))
                 from amo_sertifisering s
                          join opplaring_kategorisering_sertifisering oks
                               on oks.konsept_id = s.konsept_id
                 where oks.opplaring_kategorisering_id = ok.id),
                '[]'::jsonb)           as sertifiseringer,
       (select jsonb_build_object(
                       'utdanningsprogram', jsonb_build_object(
                       'id', up.id,
                       'navn', up.navn
                                            ),
                       'utdanninger', coalesce(
                                       jsonb_agg(
                                       distinct jsonb_build_object(
                                               'id', u.id,
                                               'navn', u.navn
                                                )
                                                ) filter (where u.id is not null),
                                       '[]'::jsonb
                                      )
               )
        from utdanningsprogram up
                 join opplaring_kategorisering_utdanning oku on oku.opplaring_kategorisering_id = ok.id
                 join utdanning u on u.id = oku.utdanning_id
        where up.id = ok.utdanningsprogram_id
        group by up.id, up.navn)       as utdanningslop
from opplaring_kategorisering ok
