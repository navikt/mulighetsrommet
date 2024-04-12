--2024-04-09 migrering drop'et view uten å endre det. Da forsvant det i prod
-- drop view if exists tiltaksgjennomforing_admin_dto_view;

create view tiltaksgjennomforing_admin_dto_view as
select gjennomforing.id,
       gjennomforing.navn,
       gjennomforing.tiltaksnummer,
       gjennomforing.start_dato,
       gjennomforing.slutt_dato,
       gjennomforing.apent_for_innsok,
       gjennomforing.sanity_id,
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
       gjennomforing.publisert and gjennomforing.avbrutt_tidspunkt is null
                                           as publisert_for_alle,
       gjennomforing.nav_region            as nav_region_enhetsnummer,
       nav_region.navn                     as nav_region_navn,
       nav_region.type                     as nav_region_type,
       nav_region.overordnet_enhet         as nav_region_overordnet_enhet,
       nav_region.status                   as nav_region_status,
       jsonb_agg(distinct
                 case
                     when tg_e.enhetsnummer is null then null::jsonb
                     else jsonb_build_object(
                             'enhetsnummer', tg_e.enhetsnummer,
                             'navn', ne.navn,
                             'type', ne.type,
                             'status', ne.status,
                             'overordnetEnhet', ne.overordnet_enhet
                          )
                     end
       )                                   as nav_enheter_json,
       gjennomforing.arena_ansvarlig_enhet as arena_nav_enhet_enhetsnummer,
       arena_nav_enhet.navn                as arena_nav_enhet_navn,
       jsonb_agg(distinct
                 case
                     when tgk.tiltaksgjennomforing_id is null then null::jsonb
                     else jsonb_build_object(
                             'navIdent', tgk.kontaktperson_nav_ident,
                             'navn', concat(na.fornavn, ' ', na.etternavn),
                             'epost', na.epost,
                             'mobilnummer', na.mobilnummer,
                             'navEnheter', tgk.enheter,
                             'hovedenhet', na.hovedenhet,
                             'beskrivelse', tgk.beskrivelse
                          )
                     end
       )                                   as nav_kontaktpersoner_json,
       jsonb_agg(
               distinct
               case
                   when tg_a.nav_ident is null then null::jsonb
                   else jsonb_build_object('navIdent', tg_a.nav_ident, 'navn',
                                           concat(na_tg.fornavn, ' ', na_tg.etternavn))
                   end
       )                                   as administratorer_json,
       arrangor.id                         as arrangor_id,
       arrangor.organisasjonsnummer        as arrangor_organisasjonsnummer,
       arrangor.navn                       as arrangor_navn,
       arrangor.slettet_dato is not null   as arrangor_slettet,
       jsonb_agg(distinct
                 case
                     when gjennomforing_arrangor_kontaktperson.tiltaksgjennomforing_id is null then null::jsonb
                     else jsonb_build_object(
                             'id', gjennomforing_arrangor_kontaktperson.arrangor_kontaktperson_id,
                             'arrangorId', arrangor_kontaktperson.arrangor_id,
                             'navn', arrangor_kontaktperson.navn,
                             'telefon', arrangor_kontaktperson.telefon,
                             'epost', arrangor_kontaktperson.epost,
                             'beskrivelse', arrangor_kontaktperson.beskrivelse
                          )
                     end
       )                                   as arrangor_kontaktpersoner_json,
       tiltakstype.id                      as tiltakstype_id,
       tiltakstype.navn                    as tiltakstype_navn,
       tiltakstype.tiltakskode             as tiltakstype_tiltakskode,
       tiltakstype.arena_kode              as tiltakstype_arena_kode,
       gjennomforing.avbrutt_tidspunkt,
       case
           when gjennomforing.avbrutt_tidspunkt is not null and gjennomforing.avbrutt_tidspunkt < gjennomforing.start_dato then 'AVLYST'
           when gjennomforing.avbrutt_tidspunkt is not null and gjennomforing.avbrutt_tidspunkt >= gjennomforing.start_dato then 'AVBRUTT'
           when gjennomforing.slutt_dato is not null and now() >= gjennomforing.slutt_dato then 'AVSLUTTET'
           when now() >= gjennomforing.start_dato then 'GJENNOMFORES'
           else 'PLANLAGT'
       end as status
from tiltaksgjennomforing gjennomforing
         inner join tiltakstype on gjennomforing.tiltakstype_id = tiltakstype.id
         left join tiltaksgjennomforing_administrator tg_a on tg_a.tiltaksgjennomforing_id = gjennomforing.id
         left join tiltaksgjennomforing_nav_enhet tg_e on tg_e.tiltaksgjennomforing_id = gjennomforing.id
         left join avtale a on a.id = gjennomforing.avtale_id
         left join nav_enhet ne on tg_e.enhetsnummer = ne.enhetsnummer
         left join nav_enhet nav_region on nav_region.enhetsnummer = gjennomforing.nav_region
         left join nav_enhet arena_nav_enhet on gjennomforing.arena_ansvarlig_enhet = arena_nav_enhet.enhetsnummer
         left join arrangor on arrangor.id = gjennomforing.arrangor_id
         left join tiltaksgjennomforing_kontaktperson tgk on tgk.tiltaksgjennomforing_id = gjennomforing.id
         left join nav_ansatt na on na.nav_ident = tgk.kontaktperson_nav_ident
         left join nav_ansatt na_tg on na_tg.nav_ident = tg_a.nav_ident
         left join tiltaksgjennomforing_arrangor_kontaktperson gjennomforing_arrangor_kontaktperson
                   on gjennomforing_arrangor_kontaktperson.tiltaksgjennomforing_id = gjennomforing.id
         left join arrangor_kontaktperson
                   on arrangor_kontaktperson.id = gjennomforing_arrangor_kontaktperson.arrangor_kontaktperson_id
group by gjennomforing.id,
         tiltakstype.id,
         arrangor.id,
         nav_region.enhetsnummer,
         arena_nav_enhet.enhetsnummer;
