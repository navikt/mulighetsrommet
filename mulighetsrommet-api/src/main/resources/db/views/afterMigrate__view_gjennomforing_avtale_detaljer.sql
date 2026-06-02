create or replace view view_gjennomforing_avtale_detaljer as
select gjennomforing.id,
       gjennomforing.status,
       gjennomforing.avbrutt_aarsaker,
       gjennomforing.avbrutt_forklaring,
       gjennomforing.publisert,
       gjennomforing.oppmote_sted,
       gjennomforing.beskrivelse,
       gjennomforing.faneinnhold,
       gjennomforing.estimert_ventetid_verdi,
       gjennomforing.estimert_ventetid_enhet,
       gjennomforing.tilgjengelig_for_arrangor_dato,
       administratorer_json,
       nav_enheter_json,
       nav_kontaktpersoner_json,
       arrangor_kontaktpersoner_json
from gjennomforing
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
                                                   'navn', concat(ansatt.fornavn, ' ', ansatt.etternavn),
                                                   'epost', ansatt.epost,
                                                   'mobilnummer', ansatt.mobilnummer,
                                                   'hovedenhet', ansatt.hovedenhet,
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
                                                   'id', id,
                                                   'navn', navn,
                                                   'telefon', telefon,
                                                   'epost', epost,
                                                   'beskrivelse', beskrivelse
                                           )
                                   ) as arrangor_kontaktpersoner_json
                            from gjennomforing_arrangor_kontaktperson
                                     join arrangor_kontaktperson kontaktperson on id = arrangor_kontaktperson_id
                            where gjennomforing_id = gjennomforing.id) on true
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
where gjennomforing.gjennomforing_type = 'AVTALE'
