drop view if exists tiltaksgjennomforing_admin_dto_view;
create view tiltaksgjennomforing_admin_dto_view as
select tg.id::uuid,
       tg.navn,
       tg.tiltakstype_id,
       tg.tiltaksnummer,
       tg.arrangor_organisasjonsnummer,
       v.navn                                                                                  as arrangor_navn,
       tg.start_dato,
       tg.slutt_dato,
       t.tiltakskode,
       t.navn                                                                                  as tiltakstype_navn,
       case
           when arena_nav_enhet.enhetsnummer
               is null then null::jsonb
           else
               jsonb_build_object(
                       'enhetsnummer', arena_nav_enhet.enhetsnummer,
                       'navn', arena_nav_enhet.navn,
                       'type', arena_nav_enhet.type,
                       'overordnetEnhet', arena_nav_enhet.overordnet_enhet) end                as arena_ansvarlig_enhet,
       tg.avslutningsstatus,
       tg.tilgjengelighet,
       tg.sanity_id,
       tg.antall_plasser,
       tg.avtale_id,
       tg.oppstart,
       tg.opphav,
       tg.stengt_fra,
       tg.stengt_til,
       tg.nav_region                                                                           as nav_region_enhetsnummer,
       region.navn                                                                             as nav_region_navn,
       region.type                                                                             as nav_region_type,
       region.overordnet_enhet                                                                 as nav_region_overordnet_enhet,
       jsonb_agg(
               distinct
               case
                   when tg_a.nav_ident is null then null::jsonb
                   else jsonb_build_object('navIdent', tg_a.nav_ident, 'navn',
                                           concat(na_tg.fornavn, ' ', na_tg.etternavn))
                   end
       )                                                                                       as administratorer,
       jsonb_agg(distinct
                 case
                     when tg_e.enhetsnummer is null then null::jsonb
                     else jsonb_build_object('enhetsnummer', tg_e.enhetsnummer, 'navn', ne.navn, 'type', ne.type,
                                             'overordnetEnhet', ne.overordnet_enhet)
                     end
       )                                                                                       as nav_enheter,
       jsonb_agg(distinct
                 case
                     when tgk.tiltaksgjennomforing_id is null then null::jsonb
                     else jsonb_build_object('navIdent', tgk.kontaktperson_nav_ident, 'navn',
                                             concat(na.fornavn, ' ', na.etternavn), 'epost', na.epost, 'mobilnummer',
                                             na.mobilnummer, 'navEnheter', tgk.enheter, 'hovedenhet', na.hovedenhet)
                     end
       )                                                                                       as kontaktpersoner,
       tg.sted_for_gjennomforing,
       tg.arrangor_kontaktperson_id,
       vk.organisasjonsnummer                                                                  as arrangor_kontaktperson_organisasjonsnummer,
       vk.navn                                                                                 as arrangor_kontaktperson_navn,
       vk.telefon                                                                              as arrangor_kontaktperson_telefon,
       vk.epost                                                                                as arrangor_kontaktperson_epost,
       vk.beskrivelse                                                                          as arrangor_kontaktperson_beskrivelse,
       t.skal_migreres,
       tg.faneinnhold,
       tg.beskrivelse,
       tg.created_at,
       tg.updated_at,
       tg.tilgjengelig_for_veileder,
       tg.tilgjengelig_for_veileder and tg.tilgjengelighet != 'STENGT'::tilgjengelighetsstatus as vises_for_veileder,
       tg.fremmote_tidspunkt,
       tg.fremmote_sted
from tiltaksgjennomforing tg
         inner join tiltakstype t on tg.tiltakstype_id = t.id
         left join tiltaksgjennomforing_administrator tg_a on tg_a.tiltaksgjennomforing_id = tg.id
         left join tiltaksgjennomforing_nav_enhet tg_e on tg_e.tiltaksgjennomforing_id = tg.id
         left join avtale a on a.id = tg.avtale_id
         left join nav_enhet ne on tg_e.enhetsnummer = ne.enhetsnummer
         left join nav_enhet region on region.enhetsnummer = tg.nav_region
         left join nav_enhet arena_nav_enhet on tg.arena_ansvarlig_enhet = arena_nav_enhet.enhetsnummer
         left join virksomhet v on v.organisasjonsnummer = tg.arrangor_organisasjonsnummer
         left join tiltaksgjennomforing_kontaktperson tgk on tgk.tiltaksgjennomforing_id = tg.id
         left join nav_ansatt na on na.nav_ident = tgk.kontaktperson_nav_ident
         left join nav_ansatt na_tg on na_tg.nav_ident = tg_a.nav_ident
         left join virksomhet_kontaktperson vk on vk.id = tg.arrangor_kontaktperson_id
group by tg.id, t.id, v.navn, vk.id, region.navn, region.type, region.overordnet_enhet, arena_nav_enhet.enhetsnummer;
