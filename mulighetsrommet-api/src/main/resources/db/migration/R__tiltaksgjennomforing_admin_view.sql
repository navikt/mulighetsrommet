create or replace view tiltaksgjennomforing_admin_dto_view as
select tg.id::uuid,
       tg.navn,
       tg.tiltakstype_id,
       tg.tiltaksnummer,
       tg.virksomhetsnummer,
       v.navn                   as virksomhetsnavn,
       tg.start_dato,
       tg.slutt_dato,
       t.tiltakskode,
       t.navn                   as tiltakstype_navn,
       tg.arena_ansvarlig_enhet,
       tg.avslutningsstatus,
       tg.tilgjengelighet,
       tg.estimert_ventetid,
       tg.sanity_id,
       tg.antall_plasser,
       tg.avtale_id,
       tg.oppstart,
       tg.opphav,
       tg.stengt_fra,
       tg.stengt_til,
       avtale_ne.navn           as navRegionForAvtale,
       array_agg(tg_a.navident) as ansvarlige,
       jsonb_agg(distinct
                 case
                     when tg_e.enhetsnummer is null then null::jsonb
                     else jsonb_build_object('enhetsnummer', tg_e.enhetsnummer, 'navn', ne.navn)
                     end
           )                    as nav_enheter,
       jsonb_agg(distinct
                 case
                     when tgk.tiltaksgjennomforing_id is null then null::jsonb
                     else jsonb_build_object('navIdent', tgk.kontaktperson_nav_ident, 'navn',
                                             concat(na.fornavn, ' ', na.etternavn), 'epost', na.epost, 'mobilnummer',
                                             na.mobilnummer, 'navEnheter', tgk.enheter, 'hovedenhet', na.hovedenhet)
                     end
           )                    as kontaktpersoner,
       tg.lokasjon_arrangor
from tiltaksgjennomforing tg
         inner join tiltakstype t on tg.tiltakstype_id = t.id
         left join tiltaksgjennomforing_ansvarlig tg_a on tg_a.tiltaksgjennomforing_id = tg.id
         left join tiltaksgjennomforing_nav_enhet tg_e on tg_e.tiltaksgjennomforing_id = tg.id
         left join avtale a on a.id = tg.avtale_id
         left join nav_enhet ne on tg_e.enhetsnummer = ne.enhetsnummer
         left join nav_enhet avtale_ne on avtale_ne.enhetsnummer = a.nav_region
         left join virksomhet v on v.organisasjonsnummer = tg.virksomhetsnummer
         left join tiltaksgjennomforing_kontaktperson tgk on tgk.tiltaksgjennomforing_id = tg.id
         left join nav_ansatt na on na.nav_ident = tgk.kontaktperson_nav_ident
group by tg.id, t.id, v.navn, avtale_ne.navn;
