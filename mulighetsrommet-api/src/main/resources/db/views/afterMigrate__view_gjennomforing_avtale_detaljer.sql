create or replace view view_gjennomforing_avtale_detaljer as
select gjennomforing.id,
       gjennomforing.status,
       gjennomforing.avbrutt_aarsaker,
       gjennomforing.avbrutt_forklaring,
       gjennomforing.publisert,
       gjennomforing.oppmote_sted,
       gjennomforing.beskrivelse,
       gjennomforing.faneinnhold,
       gjennomforing.estimert_ventetid_verdi,
       gjennomforing.estimert_ventetid_enhet,
       gjennomforing.tilgjengelig_for_arrangor_dato,
       administratorer_json,
       nav_enheter_json,
       nav_kontaktpersoner_json,
       arrangor_kontaktpersoner_json,
       opplaring_kategorisering_json
from gjennomforing
         left join lateral (select jsonb_agg(
                                           jsonb_build_object(
                                                   'enhetsnummer', enhet.enhetsnummer,
                                                   'navn', enhet.navn,
                                                   'type', enhet.type,
                                                   'overordnetEnhet', enhet.overordnet_enhet
                                           )
                                   ) as nav_enheter_json
                            from gjennomforing_nav_enhet gjennomforing_enhet
                                     join nav_enhet enhet on enhet.enhetsnummer = gjennomforing_enhet.enhetsnummer
                            where gjennomforing_id = gjennomforing.id) on true
         left join lateral (select jsonb_agg(
                                           jsonb_build_object(
                                                   'navIdent', ansatt.nav_ident,
                                                   'navn', concat(ansatt.fornavn, ' ', ansatt.etternavn),
                                                   'epost', ansatt.epost,
                                                   'mobilnummer', ansatt.mobilnummer,
                                                   'hovedenhet', ansatt.hovedenhet,
                                                   'beskrivelse', k.beskrivelse
                                           )
                                   ) as nav_kontaktpersoner_json
                            from gjennomforing_kontaktperson k
                                     join nav_ansatt ansatt on ansatt.nav_ident = k.kontaktperson_nav_ident
                            where k.gjennomforing_id = gjennomforing.id) on true
         left join lateral (select jsonb_agg(
                                           jsonb_build_object(
                                                   'navIdent', ansatt.nav_ident,
                                                   'navn', concat(ansatt.fornavn, ' ', ansatt.etternavn)
                                           )
                                   ) as administratorer_json
                            from gjennomforing_administrator administrator
                                     join nav_ansatt ansatt on ansatt.nav_ident = administrator.nav_ident
                            where administrator.gjennomforing_id = gjennomforing.id) on true
         left join lateral (select jsonb_agg(
                                           jsonb_build_object(
                                                   'id', id,
                                                   'navn', navn,
                                                   'telefon', telefon,
                                                   'epost', epost,
                                                   'beskrivelse', beskrivelse
                                           )
                                   ) as arrangor_kontaktpersoner_json
                            from gjennomforing_arrangor_kontaktperson
                                     join arrangor_kontaktperson kontaktperson on id = arrangor_kontaktperson_id
                            where gjennomforing_id = gjennomforing.id) on true
         left join lateral (select json_agg(
                                           json_build_object(
                                                   'id', id,
                                                   'start', lower(periode),
                                                   'slutt', date(upper(periode) - interval '1 day'),
                                                   'beskrivelse', beskrivelse
                                           ) order by periode
                                   ) as stengt_perioder_json
                            from gjennomforing_stengt_hos_arrangor
                            where gjennomforing_id = gjennomforing.id) on true
         left join lateral (select jsonb_build_object(
                                           'norskprove', ok.norskprove,
                                           'innholdElementer', coalesce(
                                                   (select jsonb_agg(
                                                                   jsonb_build_object(
                                                                           'id', oie.id,
                                                                           'navn', oie.navn,
                                                                           'kode', oie.kode
                                                                   )
                                                           )
                                                    from opplaring_innhold_element oie
                                                             inner join opplaring_kategorisering_innhold_element okie
                                                                        on oie.id = okie.innhold_element_id
                                                    where okie.opplaring_kategorisering_id = ok.id),
                                                   '[]'::jsonb),
                                           'kurstype', (select jsonb_strip_nulls(
                                                                       jsonb_build_object(
                                                                               'id', okk.id,
                                                                               'navn', okk.navn,
                                                                               'kode', okk.kode,
                                                                               'aktiv', okk.aktiv
                                                                       )
                                                               )
                                                        from opplaring_kategorisering_kurstype okk
                                                        where okk.id = ok.kurstype_id),
                                           'bransje', (select jsonb_strip_nulls(
                                                                      jsonb_build_object(
                                                                              'id', okb.id,
                                                                              'navn', okb.navn,
                                                                              'kode', okb.kode
                                                                      )
                                                              )
                                                       from opplaring_kategorisering_bransje okb
                                                       where okb.id = ok.bransje_id),
                                           'forerkort', coalesce(
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
                                                   '[]'::jsonb),
                                           'sertifiseringer', coalesce((select jsonb_strip_nulls(
                                                                                       jsonb_agg(
                                                                                               jsonb_build_object(
                                                                                                       'label', s.label,
                                                                                                       'konseptId',
                                                                                                       s.konsept_id
                                                                                               )
                                                                                       ))
                                                                        from amo_sertifisering s
                                                                                 join opplaring_kategorisering_sertifisering oks
                                                                                      on oks.konsept_id = s.konsept_id
                                                                        where oks.opplaring_kategorisering_id = ok.id),
                                                                       '[]'::jsonb),
                                           'utdanningslop', (select jsonb_build_object(
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
                                                                                                     )
                                                                                            filter (where u.id is not null),
                                                                                            '[]'::jsonb
                                                                                           )
                                                                    )
                                                             from utdanningsprogram up
                                                                      join opplaring_kategorisering_utdanning oku
                                                                           on oku.opplaring_kategorisering_id = ok.id
                                                                      join utdanning u on u.id = oku.utdanning_id
                                                             where up.id = ok.utdanningsprogram_id
                                                             group by up.id, up.navn)
                                   ) as opplaring_kategorisering_json
                            from opplaring_kategorisering ok
                            where ok.id = gjennomforing.id
    ) on true
where gjennomforing.gjennomforing_type = 'AVTALE'
