-- ${flyway:timestamp}

drop view if exists view_gjennomforing;

create view view_gjennomforing as
select gjennomforing.id,
       gjennomforing.gjennomforing_type,
       gjennomforing.opphav,
       gjennomforing.lopenummer,
       gjennomforing.arena_tiltaksnummer,
       gjennomforing.arena_ansvarlig_enhet as arena_nav_enhet_enhetsnummer,
       gjennomforing.navn,
       gjennomforing.start_dato,
       gjennomforing.slutt_dato,
       gjennomforing.status,
       gjennomforing.avbrutt_aarsaker,
       gjennomforing.avbrutt_forklaring,
       gjennomforing.avsluttet_tidspunkt,
       gjennomforing.deltidsprosent,
       gjennomforing.antall_plasser,
       gjennomforing.fts,
       gjennomforing.created_at            as opprettet_tidspunkt,
       gjennomforing.updated_at            as oppdatert_tidspunkt,
       gjennomforing.apent_for_pamelding,
       gjennomforing.oppstart,
       gjennomforing.pamelding_type,
       gjennomforing.beskrivelse,
       gjennomforing.faneinnhold,
       gjennomforing.estimert_ventetid_verdi,
       gjennomforing.estimert_ventetid_enhet,
       gjennomforing.oppmote_sted,
       gjennomforing.publisert,
       gjennomforing.tilgjengelig_for_arrangor_dato,
       gjennomforing.avtale_id,
       prismodell.id                       as prismodell_id,
       prismodell.valuta                   as prismodell_valuta,
       prismodell.prismodell_type          as prismodell_type,
       prismodell.prisbetingelser          as prismodell_prisbetingelser,
       prismodell.satser                   as prismodell_satser,
       tiltakstype.id                      as tiltakstype_id,
       tiltakstype.navn                    as tiltakstype_navn,
       tiltakstype.tiltakskode             as tiltakstype_tiltakskode,
       arrangor.id                         as arrangor_id,
       arrangor.organisasjonsnummer        as arrangor_organisasjonsnummer,
       arrangor.navn                       as arrangor_navn,
       arrangor.slettet_dato is not null   as arrangor_slettet,
       administratorer_json,
       nav_enheter_json,
       stengt_perioder_json
from gjennomforing
         join tiltakstype on gjennomforing.tiltakstype_id = tiltakstype.id
         join arrangor on arrangor.id = gjennomforing.arrangor_id
         left join prismodell on prismodell.id = gjennomforing.prismodell_id
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
                                                   'navn', concat(ansatt.fornavn, ' ', ansatt.etternavn)
                                           )
                                   ) as administratorer_json
                            from gjennomforing_administrator administrator
                                     join nav_ansatt ansatt on ansatt.nav_ident = administrator.nav_ident
                            where administrator.gjennomforing_id = gjennomforing.id) on true
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
