create or replace view avtale_admin_dto_view as
select a.id,
       a.navn,
       a.tiltakstype_id,
       a.avtalenummer,
       v.id                       as leverandor_virksomhet_id,
       v.organisasjonsnummer      as leverandor_organisasjonsnummer,
       v.navn                     as leverandor_navn,
       v.slettet_dato is not null as leverandor_slettet,
       vk.id                      as leverandor_kontaktperson_id,
       vk.virksomhet_id           as leverandor_kontaktperson_virksomhet_id,
       vk.navn                    as leverandor_kontaktperson_navn,
       vk.telefon                 as leverandor_kontaktperson_telefon,
       vk.epost                   as leverandor_kontaktperson_epost,
       vk.beskrivelse             as leverandor_kontaktperson_beskrivelse,
       a.start_dato,
       a.slutt_dato,
       case
           when arena_nav_enhet.enhetsnummer is null then null::jsonb
           else
               jsonb_build_object(
                       'enhetsnummer', arena_nav_enhet.enhetsnummer,
                       'navn', arena_nav_enhet.navn,
                       'type', arena_nav_enhet.type,
                       'overordnetEnhet', arena_nav_enhet.overordnet_enhet,
                       'status', arena_nav_enhet.status
               ) end              as arena_ansvarlig_enhet,
       a.opphav,
       a.avtaletype,
       a.avslutningsstatus,
       a.prisbetingelser,
       a.antall_plasser,
       a.url,
       a.updated_at,
       t.navn                     as tiltakstype_navn,
       t.tiltakskode,
       an.nav_enheter,
       lu.leverandor_underenheter,
       jsonb_agg(
               distinct
               case
                   when aa.nav_ident is null then null::jsonb
                   else jsonb_build_object('navIdent', aa.nav_ident, 'navn', concat(na.fornavn, ' ', na.etternavn))
                   end
       )                          as administratorer,
       a.beskrivelse,
       a.faneinnhold
from avtale a
         join tiltakstype t on t.id = a.tiltakstype_id
         left join avtale_administrator aa on a.id = aa.avtale_id
         left join nav_ansatt na on na.nav_ident = aa.nav_ident
         left join nav_enhet arena_nav_enhet on a.arena_ansvarlig_enhet = arena_nav_enhet.enhetsnummer
         left join lateral (
    select an.avtale_id,
           jsonb_strip_nulls(jsonb_agg(jsonb_build_object(
                   'enhetsnummer', an.enhetsnummer,
                   'navn', ne.navn,
                   'type', ne.type,
                   'status', ne.status,
                   'overordnetEnhet', ne.overordnet_enhet))) as nav_enheter
    from avtale_nav_enhet an
             left join nav_enhet ne on ne.enhetsnummer = an.enhetsnummer
    group by an.avtale_id
    ) an on an.avtale_id = a.id
         left join lateral (
    select au.avtale_id,
           jsonb_strip_nulls(jsonb_agg(jsonb_build_object(
                   'id', v.id,
                   'organisasjonsnummer', v.organisasjonsnummer,
                   'navn', v.navn,
                   'slettet', v.slettet_dato is not null))) as leverandor_underenheter
    from avtale_underleverandor au
             left join virksomhet v on v.id = au.virksomhet_id
    group by au.avtale_id
    ) lu on lu.avtale_id = a.id
         left join virksomhet v on v.id = a.leverandor_virksomhet_id
         left join virksomhet_kontaktperson vk on vk.id = a.leverandor_kontaktperson_id
group by a.id,
         t.navn,
         t.tiltakskode,
         v.organisasjonsnummer,
         v.id,
         vk.id,
         lu.leverandor_underenheter,
         an.nav_enheter,
         arena_nav_enhet.enhetsnummer;
