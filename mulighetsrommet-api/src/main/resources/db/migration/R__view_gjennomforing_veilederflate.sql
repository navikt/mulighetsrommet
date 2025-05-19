-- ${flyway:timestamp}

drop view if exists veilederflate_tiltak_view;

create view veilederflate_tiltak_view as
select gjennomforing.id,
       gjennomforing.fts,
       gjennomforing.navn,
       gjennomforing.sted_for_gjennomforing,
       gjennomforing.apent_for_pamelding,
       gjennomforing.tiltaksnummer,
       gjennomforing.oppstart,
       gjennomforing.start_dato,
       gjennomforing.slutt_dato,
       gjennomforing.status,
       gjennomforing.estimert_ventetid_verdi,
       gjennomforing.estimert_ventetid_enhet,
       gjennomforing.beskrivelse,
       gjennomforing.faneinnhold,
       gjennomforing.publisert,
       tiltakstype.id                                                 as tiltakstype_id,
       tiltakstype.sanity_id                                          as tiltakstype_sanity_id,
       tiltakstype.navn                                               as tiltakstype_navn,
       tiltakstype.tiltakskode                                        as tiltakstype_tiltakskode,
       tiltakstype.innsatsgrupper                                     as tiltakstype_innsatsgrupper,
       avtale.personvern_bekreftet,
       personopplysninger_som_kan_behandles,
       nav_enheter_json,
       nav_kontaktpersoner_json,
       arrangor.id                                                    as arrangor_id,
       arrangor.organisasjonsnummer                                   as arrangor_organisasjonsnummer,
       arrangor.navn                                                  as arrangor_navn,
       arrangor_kontaktpersoner_json
from gjennomforing
         join tiltakstype on gjennomforing.tiltakstype_id = tiltakstype.id
         left join avtale on avtale.id = gjennomforing.avtale_id
         left join arrangor on arrangor.id = gjennomforing.arrangor_id
         left join lateral (select array_agg(personopplysning) as personopplysninger_som_kan_behandles
                            from avtale_personopplysning
                            where avtale_id = avtale.id) on true
         left join lateral (select jsonb_agg(
                                           jsonb_build_object(
                                                   'enhetsnummer', gjennomforing_nav_enhet.enhetsnummer,
                                                   'type', nav_enhet.type
                                           )
                                   ) as nav_enheter_json
                                           from gjennomforing_nav_enhet
                                           left join nav_enhet on nav_enhet.enhetsnummer = gjennomforing_nav_enhet.enhetsnummer
                                           where gjennomforing_nav_enhet.gjennomforing_id = gjennomforing.id) on true
         left join lateral (select jsonb_agg(
                                           jsonb_build_object(
                                                   'navn', concat(nav_ansatt.fornavn, ' ', nav_ansatt.etternavn),
                                                   'telefon', nav_ansatt.mobilnummer,
                                                   'enhet',
                                                   json_build_object(
                                                           'navn', nav_enhet.navn,
                                                           'enhetsnummer', nav_enhet.enhetsnummer
                                                   ),
                                                   'epost', nav_ansatt.epost,
                                                   'beskrivelse', k.beskrivelse
                                           )
                                   ) as nav_kontaktpersoner_json
                            from gjennomforing_kontaktperson k
                                     join nav_ansatt on nav_ansatt.nav_ident = k.kontaktperson_nav_ident
                                     join nav_enhet on nav_enhet.enhetsnummer = nav_ansatt.hovedenhet
                            where gjennomforing_id = gjennomforing.id) on true
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
                                     left join arrangor_kontaktperson on id = arrangor_kontaktperson_id
                            where gjennomforing_id = gjennomforing.id) on true
