-- ${flyway:timestamp}

drop view if exists gjennomforing_admin_dto_view;

create view gjennomforing_admin_dto_view as
select gjennomforing.id,
       gjennomforing.fts,
       gjennomforing.navn,
       gjennomforing.tiltaksnummer,
       gjennomforing.start_dato,
       gjennomforing.slutt_dato,
       gjennomforing.apent_for_pamelding,
       gjennomforing.antall_plasser,
       gjennomforing.avtale_id,
       gjennomforing.oppstart,
       gjennomforing.opphav,
       gjennomforing.beskrivelse,
       gjennomforing.faneinnhold,
       gjennomforing.created_at                                       as opprettet_tidspunkt,
       gjennomforing.updated_at                                       as oppdatert_tidspunkt,
       gjennomforing.deltidsprosent,
       gjennomforing.estimert_ventetid_verdi,
       gjennomforing.estimert_ventetid_enhet,
       gjennomforing.sted_for_gjennomforing,
       gjennomforing.publisert,
       gjennomforing.lopenummer,
       gjennomforing.arena_ansvarlig_enhet                            as arena_nav_enhet_enhetsnummer,
       arena_nav_enhet.navn                                           as arena_nav_enhet_navn,
       gjennomforing.avsluttet_tidspunkt,
       gjennomforing.avbrutt_aarsak,
       gjennomforing.tilgjengelig_for_arrangor_dato,
       tiltaksgjennomforing_status(gjennomforing.start_dato,
                                   gjennomforing.slutt_dato,
                                   gjennomforing.avsluttet_tidspunkt) as status,
       nav_kontaktpersoner_json,
       administratorer_json,
       koordinator_json,
       nav_enheter_json,
       amo_kategorisering_json,
       tiltakstype.id                                                 as tiltakstype_id,
       tiltakstype.navn                                               as tiltakstype_navn,
       tiltakstype.tiltakskode                                        as tiltakstype_tiltakskode,
       arrangor.id                                                    as arrangor_id,
       arrangor.organisasjonsnummer                                   as arrangor_organisasjonsnummer,
       arrangor.navn                                                  as arrangor_navn,
       arrangor.slettet_dato is not null                              as arrangor_slettet,
       arrangor_kontaktpersoner_json,
       utdanningslop_json,
       stengt_perioder_json
from gjennomforing
         join tiltakstype on gjennomforing.tiltakstype_id = tiltakstype.id
         join arrangor on arrangor.id = gjennomforing.arrangor_id
         left join nav_enhet arena_nav_enhet on gjennomforing.arena_ansvarlig_enhet = arena_nav_enhet.enhetsnummer
         left join lateral (select jsonb_agg(
                                           jsonb_build_object(
                                                   'enhetsnummer', enhet.enhetsnummer,
                                                   'navn', enhet.navn,
                                                   'type', enhet.type,
                                                   'status', enhet.status,
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
                                                   'navEnheter', k.enheter,
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
                                                   'navIdent', ansatt.nav_ident,
                                                   'navn', concat(ansatt.fornavn, ' ', ansatt.etternavn)
                                           )
                                   ) as koordinator_json
                            from gjennomforing_koordinator koordinator
                                     join nav_ansatt ansatt on ansatt.nav_ident = koordinator.nav_ident
                            where koordinator.gjennomforing_id = gjennomforing.id) on true
         left join lateral (select jsonb_agg(
                                           jsonb_build_object(
                                                   'id', id,
                                                   'arrangorId', arrangor_id,
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
                                           'innholdElementer', k.innhold_elementer
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
                            where gjennomforing_id = gjennomforing.id) on true;

