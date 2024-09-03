drop view if exists avtale_admin_dto_view;

create view avtale_admin_dto_view as
select avtale.id,
       avtale.navn,
       coalesce(avtale.avtalenummer, avtale.lopenummer) as avtalenummer,
       avtale.websaknummer,
       avtale.start_dato,
       avtale.slutt_dato,
       avtale.opsjon_maks_varighet,
       avtale.opphav,
       avtale.avtaletype,
       avtale.avbrutt_tidspunkt,
       avtale.prisbetingelser,
       avtale.antall_plasser,
       avtale.beskrivelse,
       avtale.faneinnhold,
       avtale.opsjonsmodell,
       avtale.opsjon_custom_opsjonsmodell_navn,
       avtale.personvern_bekreftet,
       avtale.avbrutt_aarsak,
       avtale.arena_ansvarlig_enhet                     as arena_nav_enhet_enhetsnummer,
       arena_nav_enhet.navn                             as arena_nav_enhet_navn,
       case
           when avtale.avbrutt_tidspunkt is not null then 'AVBRUTT'
           when avtale.slutt_dato is not null and date(now()) > avtale.slutt_dato then 'AVSLUTTET'
           else 'AKTIV'
           end                                          as status,
       tiltakstype.id                                   as tiltakstype_id,
       tiltakstype.navn                                 as tiltakstype_navn,
       tiltakstype.tiltakskode                          as tiltakstype_tiltakskode,
       tiltakstype.arena_kode                           as tiltakstype_arena_kode,
       personopplysninger_json,
       administratorer_json,
       nav_enheter_json,
       opsjon_logg_json,
       amo_kategorisering_json,
       arrangor.id                                      as arrangor_hovedenhet_id,
       arrangor.organisasjonsnummer                     as arrangor_hovedenhet_organisasjonsnummer,
       arrangor.navn                                    as arrangor_hovedenhet_navn,
       arrangor.slettet_dato is not null                as arrangor_hovedenhet_slettet,
       arrangor_underenheter_json,
       arrangor_kontaktpersoner_json
from avtale
         join tiltakstype on tiltakstype.id = avtale.tiltakstype_id
         join arrangor on arrangor.id = avtale.arrangor_hovedenhet_id
         left join nav_enhet arena_nav_enhet on avtale.arena_ansvarlig_enhet = arena_nav_enhet.enhetsnummer
         left join lateral (select jsonb_agg(personopplysning) as personopplysninger_json
                            from avtale_personopplysning
                            where avtale_id = avtale.id) on true
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
                                                   'status', nav_enhet.status,
                                                   'overordnetEnhet', nav_enhet.overordnet_enhet
                                           )
                                   ) as nav_enheter_json
                            from avtale_nav_enhet
                                     left join nav_enhet on nav_enhet.enhetsnummer = avtale_nav_enhet.enhetsnummer
                            where avtale_nav_enhet.avtale_id = avtale.id) on true
         left join lateral (select jsonb_agg(
                                           jsonb_build_object('id', avtale_opsjon_logg.id,
                                                              'aktivertDato', avtale_opsjon_logg.registrert_dato,
                                                              'sluttDato', avtale_opsjon_logg.sluttdato,
                                                              'status', avtale_opsjon_logg.status,
                                                              'forrigeSluttdato', avtale_opsjon_logg.forrige_sluttdato
                                           )) as opsjon_logg_json
                            from avtale_opsjon_logg
                            where avtale_id = avtale.id) on true
         left join lateral (select jsonb_build_object(
                                           'kurstype', avtale_amo_kategorisering.kurstype,
                                           'bransje', avtale_amo_kategorisering.bransje,
                                           'forerkort', avtale_amo_kategorisering.forerkort,
                                           'norskprove', avtale_amo_kategorisering.norskprove,
                                           'sertifiseringer',
                                           coalesce((select jsonb_strip_nulls(
                                                                    jsonb_agg(
                                                                            jsonb_build_object(
                                                                                    'label', s.label,
                                                                                    'konseptId', s.konsept_id
                                                                            )
                                                                    ))
                                                     from amo_sertifisering s
                                                              join avtale_amo_kategorisering_sertifisering aks
                                                                   on aks.konsept_id = s.konsept_id
                                                     where aks.avtale_id = avtale_amo_kategorisering.avtale_id),
                                                    '[]'::jsonb),
                                           'innholdElementer', avtale_amo_kategorisering.innhold_elementer
                                   ) as amo_kategorisering_json
                            from avtale_amo_kategorisering
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
                                                   'arrangorId', arrangor_kontaktperson.arrangor_id,
                                                   'navn', arrangor_kontaktperson.navn,
                                                   'telefon', arrangor_kontaktperson.telefon,
                                                   'epost', arrangor_kontaktperson.epost,
                                                   'beskrivelse', arrangor_kontaktperson.beskrivelse,
                                                   'ansvarligFor', arrangor_kontaktperson.ansvarlig_for
                                           )
                                   ) arrangor_kontaktpersoner_json
                            from avtale_arrangor_kontaktperson
                                     join arrangor_kontaktperson
                                          on avtale_arrangor_kontaktperson.arrangor_kontaktperson_id =
                                             arrangor_kontaktperson.id
                            where avtale_id = avtale.id) on true
