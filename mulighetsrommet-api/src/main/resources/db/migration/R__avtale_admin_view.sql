drop view if exists avtale_admin_dto_view;

create view avtale_admin_dto_view as
select avtale.id,
       avtale.navn,
       avtale.avtalenummer,
       avtale.start_dato,
       avtale.slutt_dato,
       avtale.opphav,
       avtale.avtaletype,
       avtale.avbrutt_tidspunkt,
       avtale.prisbetingelser,
       avtale.antall_plasser,
       avtale.url,
       avtale.beskrivelse,
       avtale.faneinnhold,
       jsonb_agg(
               distinct
               case
                   when avtale_administrator.nav_ident is null then null::jsonb
                   else jsonb_build_object('navIdent', avtale_administrator.nav_ident,
                                           'navn', concat(nav_ansatt.fornavn, ' ', nav_ansatt.etternavn))
                   end
       )                                   as administratorer_json,
       avtale_nav_enheter_json.nav_enheter as nav_enheter_json,
       avtale.arena_ansvarlig_enhet        as arena_nav_enhet_enhetsnummer,
       arena_nav_enhet.navn                as arena_nav_enhet_navn,
       arrangor.id                         as arrangor_hovedenhet_id,
       arrangor.organisasjonsnummer        as arrangor_hovedenhet_organisasjonsnummer,
       arrangor.navn                       as arrangor_hovedenhet_navn,
       arrangor.slettet_dato is not null   as arrangor_hovedenhet_slettet,
       arrangor_kontaktperson.id           as arrangor_hovedenhet_kontaktperson_id,
       arrangor_kontaktperson.arrangor_id  as arrangor_hovedenhet_kontaktperson_arrangor_id,
       arrangor_kontaktperson.navn         as arrangor_hovedenhet_kontaktperson_navn,
       arrangor_kontaktperson.telefon      as arrangor_hovedenhet_kontaktperson_telefon,
       arrangor_kontaktperson.epost        as arrangor_hovedenhet_kontaktperson_epost,
       arrangor_kontaktperson.beskrivelse  as arrangor_hovedenhet_kontaktperson_beskrivelse,
       arrangor_underenheter_json.arrangor_underenheter,
       tiltakstype.id                      as tiltakstype_id,
       tiltakstype.navn                    as tiltakstype_navn,
       tiltakstype.tiltakskode             as tiltakstype_tiltakskode,
       tiltakstype.arena_kode              as tiltakstype_arena_kode
from avtale
         join tiltakstype on tiltakstype.id = avtale.tiltakstype_id
         left join avtale_administrator on avtale.id = avtale_administrator.avtale_id
         left join nav_ansatt on nav_ansatt.nav_ident = avtale_administrator.nav_ident
         left join nav_enhet arena_nav_enhet on avtale.arena_ansvarlig_enhet = arena_nav_enhet.enhetsnummer
         left join arrangor on arrangor.id = avtale.arrangor_hovedenhet_id
         left join arrangor_kontaktperson on arrangor_kontaktperson.id = avtale.arrangor_kontaktperson_id
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
    ) avtale_nav_enheter_json on avtale_nav_enheter_json.avtale_id = avtale.id
         left join lateral (
    select au.avtale_id,
           jsonb_strip_nulls(jsonb_agg(jsonb_build_object(
                   'id', v.id,
                   'organisasjonsnummer', v.organisasjonsnummer,
                   'navn', v.navn,
                   'slettet', v.slettet_dato is not null))) as arrangor_underenheter
    from avtale_arrangor_underenhet au
             left join arrangor v on v.id = au.arrangor_id
    group by au.avtale_id
    ) arrangor_underenheter_json on arrangor_underenheter_json.avtale_id = avtale.id
group by avtale.id,
         tiltakstype.id,
         arrangor.id,
         arrangor_kontaktperson.id,
         arrangor_underenheter_json.arrangor_underenheter,
         avtale_nav_enheter_json.nav_enheter,
         arena_nav_enhet.enhetsnummer;
