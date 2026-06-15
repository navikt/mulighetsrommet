create or replace view view_avtale as
select avtale.id,
       avtale.created_at                                as opprettet_tidspunkt,
       avtale.updated_at                                as oppdatert_tidspunkt,
       avtale.fts,
       avtale.navn,
       coalesce(avtale.avtalenummer, avtale.lopenummer) as avtalenummer,
       avtale.sakarkiv_nummer,
       avtale.start_dato,
       avtale.slutt_dato,
       avtale.opsjon_maks_varighet,
       avtale.avtaletype,
       avtale.avbrutt_tidspunkt,
       avtale.avbrutt_aarsaker,
       avtale.avbrutt_forklaring,
       avtale.status,
       avtale.beskrivelse,
       avtale.faneinnhold,
       avtale.opsjonsmodell,
       avtale.opsjon_custom_opsjonsmodell_navn,
       avtale.personvern_bekreftet,
       avtale.arena_ansvarlig_enhet                     as arena_nav_enhet_enhetsnummer,
       arena_nav_enhet.navn                             as arena_nav_enhet_navn,
       tiltakstype.id                                   as tiltakstype_id,
       tiltakstype.navn                                 as tiltakstype_navn,
       tiltakstype.tiltakskode                          as tiltakstype_tiltakskode,
       tiltakstype.arena_kode                           as tiltakstype_arena_kode,
       personopplysninger_json,
       administratorer_json,
       nav_enheter_json,
       opsjon_logg_json,
       arrangor.id                                      as arrangor_hovedenhet_id,
       arrangor.organisasjonsnummer                     as arrangor_hovedenhet_organisasjonsnummer,
       arrangor.navn                                    as arrangor_hovedenhet_navn,
       arrangor.slettet_dato is not null                as arrangor_hovedenhet_slettet,
       arrangor_underenheter_json,
       arrangor_kontaktpersoner_json,
       coalesce(prismodeller_json, '[]'::jsonb)         as prismodeller_json,
       opplaring_kategorisering_json                    as opplaring_kategorisering_json
from avtale
         join tiltakstype on tiltakstype.id = avtale.tiltakstype_id
         left join arrangor on arrangor.id = avtale.arrangor_hovedenhet_id
         left join nav_enhet arena_nav_enhet on avtale.arena_ansvarlig_enhet = arena_nav_enhet.enhetsnummer
         left join lateral (select jsonb_agg(
                                           jsonb_build_object(
                                                   'id', id,
                                                   'systemId', system_id,
                                                   'type', prismodell_type,
                                                   'valuta', valuta,
                                                   'prisbetingelser', prisbetingelser,
                                                   'satser', satser,
                                                   'tilsagnPerDeltaker', tilsagn_per_deltaker,
                                                   'totalbelop', totalbelop,
                                                   'tilskudd', tilskudd,
                                                   'aarsak', aarsak
                                           )
                                           order by id
                                   ) as prismodeller_json
                            from avtale_prismodell
                                     join prismodell on prismodell_id = prismodell.id
                            where avtale_id = avtale.id) on true
         left join lateral (
    select jsonb_agg(
                   jsonb_build_object(
                           'type', ap.personopplysning,
                           'title', p.title,
                           'helpText', p.help_text,
                           'sortKey', p.sort_key
                   ) order by p.sort_key
           ) as personopplysninger_json
    from avtale_personopplysning ap
             join personopplysning p on p.value = ap.personopplysning
    where ap.avtale_id = avtale.id
    ) on true
         left join lateral (select jsonb_agg(
                                           jsonb_build_object(
                                                   'navIdent', avtale_administrator.nav_ident,
                                                   'navn', concat(nav_ansatt.fornavn, ' ', nav_ansatt.etternavn)
                                           )
                                   ) as administratorer_json
                            from avtale_administrator
                                     join nav_ansatt on nav_ansatt.nav_ident = avtale_administrator.nav_ident
                            where avtale_administrator.avtale_id = avtale.id) on true
         left join lateral (select jsonb_agg(
                                           jsonb_build_object(
                                                   'enhetsnummer', avtale_nav_enhet.enhetsnummer,
                                                   'navn', nav_enhet.navn,
                                                   'type', nav_enhet.type,
                                                   'overordnetEnhet', nav_enhet.overordnet_enhet
                                           )
                                   ) as nav_enheter_json
                            from avtale_nav_enhet
                                     left join nav_enhet on nav_enhet.enhetsnummer = avtale_nav_enhet.enhetsnummer
                            where avtale_nav_enhet.avtale_id = avtale.id) on true
         left join lateral (select jsonb_agg(
                                           jsonb_build_object('id', avtale_opsjon_logg.id,
                                                              'createdAt', avtale_opsjon_logg.created_at::timestamp,
                                                              'sluttDato', avtale_opsjon_logg.sluttdato,
                                                              'status', avtale_opsjon_logg.status,
                                                              'forrigeSluttDato', avtale_opsjon_logg.forrige_sluttdato
                                           )) as opsjon_logg_json
                            from avtale_opsjon_logg
                            where avtale_id = avtale.id) on true
         left join lateral (select jsonb_agg(
                                           jsonb_build_object(
                                                   'id', arrangor.id,
                                                   'organisasjonsnummer', arrangor.organisasjonsnummer,
                                                   'navn', arrangor.navn,
                                                   'slettet', arrangor.slettet_dato is not null
                                           )
                                   ) as arrangor_underenheter_json
                            from avtale_arrangor_underenhet
                                     join arrangor on avtale_arrangor_underenhet.arrangor_id = arrangor.id
                            where avtale_id = avtale.id ) on true
         left join lateral (select jsonb_agg(
                                           jsonb_build_object(
                                                   'id', avtale_arrangor_kontaktperson.arrangor_kontaktperson_id,
                                                   'navn', arrangor_kontaktperson.navn,
                                                   'telefon', arrangor_kontaktperson.telefon,
                                                   'epost', arrangor_kontaktperson.epost,
                                                   'beskrivelse', arrangor_kontaktperson.beskrivelse
                                           )
                                   ) arrangor_kontaktpersoner_json
                            from avtale_arrangor_kontaktperson
                                     join arrangor_kontaktperson
                                          on avtale_arrangor_kontaktperson.arrangor_kontaktperson_id =
                                             arrangor_kontaktperson.id
                            where avtale_id = avtale.id) on true
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
                            where id = avtale.id
    ) on true
