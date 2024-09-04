drop view if exists tiltaksgjennomforing_admin_dto_view;

create view tiltaksgjennomforing_admin_dto_view as
select gjennomforing.id,
       gjennomforing.fts,
       gjennomforing.navn,
       gjennomforing.tiltaksnummer,
       gjennomforing.start_dato,
       gjennomforing.slutt_dato,
       gjennomforing.apent_for_innsok,
       gjennomforing.antall_plasser,
       gjennomforing.avtale_id,
       gjennomforing.oppstart,
       gjennomforing.opphav,
       gjennomforing.beskrivelse,
       gjennomforing.faneinnhold,
       gjennomforing.created_at,
       gjennomforing.deltidsprosent,
       gjennomforing.estimert_ventetid_verdi,
       gjennomforing.estimert_ventetid_enhet,
       gjennomforing.sted_for_gjennomforing,
       gjennomforing.publisert,
       gjennomforing.nav_region                                     as nav_region_enhetsnummer,
       nav_region.navn                                              as nav_region_navn,
       nav_region.type                                              as nav_region_type,
       nav_region.overordnet_enhet                                  as nav_region_overordnet_enhet,
       nav_region.status                                            as nav_region_status,
       gjennomforing.arena_ansvarlig_enhet                          as arena_nav_enhet_enhetsnummer,
       arena_nav_enhet.navn                                         as arena_nav_enhet_navn,
       gjennomforing.avbrutt_tidspunkt,
       gjennomforing.avbrutt_aarsak,
       gjennomforing.tilgjengelig_for_arrangor_fra_og_med_dato,
       tiltaksgjennomforing_status(gjennomforing.start_dato,
                                   gjennomforing.slutt_dato,
                                   gjennomforing.avbrutt_tidspunkt) as status,
       nav_kontaktpersoner_json,
       administratorer_json,
       nav_enheter_json,
       amo_kategorisering_json,
       tiltakstype.id                                               as tiltakstype_id,
       tiltakstype.navn                                             as tiltakstype_navn,
       tiltakstype.tiltakskode                                      as tiltakstype_tiltakskode,
       arrangor.id                                                  as arrangor_id,
       arrangor.organisasjonsnummer                                 as arrangor_organisasjonsnummer,
       arrangor.navn                                                as arrangor_navn,
       arrangor.slettet_dato is not null                            as arrangor_slettet,
       arrangor_kontaktpersoner_json
from tiltaksgjennomforing gjennomforing
         join tiltakstype on gjennomforing.tiltakstype_id = tiltakstype.id
         join arrangor on arrangor.id = gjennomforing.arrangor_id
         left join nav_enhet nav_region on nav_region.enhetsnummer = gjennomforing.nav_region
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
                            from tiltaksgjennomforing_nav_enhet
                                     natural join nav_enhet enhet
                            where tiltaksgjennomforing_id = gjennomforing.id) on true
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
                            from tiltaksgjennomforing_kontaktperson k
                                     join nav_ansatt ansatt on ansatt.nav_ident = k.kontaktperson_nav_ident
                            where k.tiltaksgjennomforing_id = gjennomforing.id) on true
         left join lateral (select jsonb_agg(
                                           jsonb_build_object(
                                                   'navIdent', ansatt.nav_ident,
                                                   'navn', concat(ansatt.fornavn, ' ', ansatt.etternavn)
                                           )
                                   ) as administratorer_json
                            from tiltaksgjennomforing_administrator administrator
                                     natural join nav_ansatt ansatt
                            where administrator.tiltaksgjennomforing_id = gjennomforing.id) on true
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
                            from tiltaksgjennomforing_arrangor_kontaktperson
                                     join arrangor_kontaktperson kontaktperson on id = arrangor_kontaktperson_id
                            where tiltaksgjennomforing_id = gjennomforing.id) on true
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
                                                              join tiltaksgjennomforing_amo_kategorisering_sertifisering aks
                                                                   on aks.konsept_id = s.konsept_id
                                                     where aks.tiltaksgjennomforing_id = k.tiltaksgjennomforing_id),
                                                    '[]'::jsonb),
                                           'innholdElementer', k.innhold_elementer
                                   ) as amo_kategorisering_json
                            from tiltaksgjennomforing_amo_kategorisering k
                            where tiltaksgjennomforing_id = gjennomforing.id
    ) on true;
