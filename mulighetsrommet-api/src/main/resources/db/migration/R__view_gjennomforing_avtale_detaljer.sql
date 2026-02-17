-- ${flyway:timestamp}

drop view if exists view_gjennomforing_avtale_detaljer;

create view view_gjennomforing_avtale_detaljer as
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
       utdanningslop_json,
       amo_kategorisering_json
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
         left join lateral (select jsonb_build_object(
                                           'kurstype', k.kurstype,
                                           'bransje', k.bransje,
                                           'forerkort', k.forerkort,
                                           'norskprove', k.norskprove,
                                           'sertifiseringer',
                                           coalesce((select jsonb_strip_nulls(
                                                                    jsonb_agg(
                                                                            jsonb_build_object(
                                                                                    'label',
                                                                                    s.label,
                                                                                    'konseptId',
                                                                                    s.konsept_id
                                                                            )
                                                                    )
                                                            )
                                                     from amo_sertifisering s
                                                              join gjennomforing_amo_kategorisering_sertifisering aks
                                                                   on aks.konsept_id = s.konsept_id
                                                     where aks.gjennomforing_id = k.gjennomforing_id),
                                                    '[]'::jsonb),
                                           'innholdElementer', coalesce(k.innhold_elementer, '{}')
                                   ) as amo_kategorisering_json
                            from gjennomforing_amo_kategorisering k
                            where gjennomforing_id = gjennomforing.id) on true
         left join lateral (select jsonb_build_object(
                                           'utdanningsprogram',
                                           json_build_object('id', up.id, 'navn', up.navn),
                                           'utdanninger',
                                           jsonb_agg(jsonb_build_object('id', u.id, 'navn', u.navn))
                                   ) utdanningslop_json
                            from gjennomforing t
                                     join gjennomforing_utdanningsprogram upt
                                          on t.id = upt.gjennomforing_id
                                     join utdanningsprogram up on upt.utdanningsprogram_id = up.id
                                     join utdanning u on upt.utdanning_id = u.id
                            where gjennomforing_id = gjennomforing.id
                            group by up.id) on true
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
where gjennomforing.gjennomforing_type = 'AVTALE'
