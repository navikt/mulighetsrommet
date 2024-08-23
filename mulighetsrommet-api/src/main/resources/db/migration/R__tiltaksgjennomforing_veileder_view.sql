drop view if exists tiltaksgjennomforing_veileder_dto_view;

create view tiltaksgjennomforing_veileder_dto_view as
select gjennomforing.id,
       gjennomforing.avtale_id,
       gjennomforing.navn,
       gjennomforing.sted_for_gjennomforing,
       gjennomforing.apent_for_innsok,
       gjennomforing.tiltaksnummer,
       gjennomforing.oppstart,
       gjennomforing.start_dato,
       gjennomforing.slutt_dato,
       gjennomforing.estimert_ventetid_verdi,
       gjennomforing.estimert_ventetid_enhet,
       gjennomforing.beskrivelse,
       gjennomforing.faneinnhold,
       gjennomforing.nav_region,
       gjennomforing.avbrutt_aarsak,
       gjennomforing.avbrutt_tidspunkt,
       tiltaksgjennomforing_status(gjennomforing.start_dato,
                                   gjennomforing.slutt_dato,
                                   gjennomforing.avbrutt_tidspunkt) as status,
       tiltakstype.sanity_id                                        as tiltakstype_sanity_id,
       tiltakstype.navn                                             as tiltakstype_navn,
       tiltakstype.innsatsgrupper                                   as tiltakstype_innsatsgrupper,
       avtale.personvern_bekreftet,
       personopplysninger_som_kan_behandles,
       nav_enheter,
       nav_kontaktpersoner_json,
       arrangor.id                                                  as arrangor_id,
       arrangor.organisasjonsnummer                                 as arrangor_organisasjonsnummer,
       arrangor.navn                                                as arrangor_navn,
       arrangor_kontaktpersoner_json
from tiltaksgjennomforing gjennomforing
         join tiltakstype on gjennomforing.tiltakstype_id = tiltakstype.id
         left join avtale on avtale.id = gjennomforing.avtale_id
         left join arrangor on arrangor.id = gjennomforing.arrangor_id
         left join lateral (select array_agg(personopplysning) as personopplysninger_som_kan_behandles
                            from avtale_personopplysning
                            where avtale_id = avtale.id) avtale_personvern on true
         left join lateral (select array_agg(enhetsnummer) as nav_enheter
                            from tiltaksgjennomforing_nav_enhet
                            where tiltaksgjennomforing_id = gjennomforing.id) gjennomforing_nav_enheter on true
         left join lateral (select jsonb_agg(
                                           jsonb_build_object(
                                                   'navn', concat(nav_ansatt.fornavn, ' ', nav_ansatt.etternavn),
                                                   'telefon', nav_ansatt.mobilnummer,
                                                   'enhet',
                                                   json_build_object(
                                                           'navn', nav_enhet.navn,
                                                           'enhetsnummer', nav_enhet.enhetsnummer,
                                                           'status', nav_enhet.status,
                                                           'type', nav_enhet.type,
                                                           'overordnetEnhet',
                                                           nav_enhet.overordnet_enhet
                                                   ),
                                                   'epost', nav_ansatt.epost,
                                                   'beskrivelse', k.beskrivelse
                                           )
                                   ) as nav_kontaktpersoner_json
                            from tiltaksgjennomforing_kontaktperson k
                                     join nav_ansatt on nav_ansatt.nav_ident = k.kontaktperson_nav_ident
                                     join nav_enhet on nav_enhet.enhetsnummer = nav_ansatt.hovedenhet
                            where tiltaksgjennomforing_id = gjennomforing.id) nav_kontaktpersoner on true
         left join lateral (select jsonb_agg(
                                           jsonb_build_object(
                                                   'id', id,
                                                   'navn', navn,
                                                   'telefon', telefon,
                                                   'epost', epost,
                                                   'beskrivelse', beskrivelse
                                           )
                                   ) as arrangor_kontaktpersoner_json
                            from tiltaksgjennomforing_arrangor_kontaktperson
                                     left join arrangor_kontaktperson on id = arrangor_kontaktperson_id
                            where tiltaksgjennomforing_id = gjennomforing.id) arrangor_kontaktpersoner on true
where gjennomforing.publisert
