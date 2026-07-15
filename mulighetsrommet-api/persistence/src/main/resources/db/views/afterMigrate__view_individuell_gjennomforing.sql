create or replace view view_individuell_gjennomforing as
select ig.id,
       ig.navn,
       ig.tiltakstype_id,
       ig.sted_for_gjennomforing,
       ig.arrangor_id,
       ig.faneinnhold,
       ig.beskrivelse,
       ig.created_at,
       ig.updated_at,
       tiltakstype.navn              as tiltakstype_navn,
       tiltakstype.tiltakskode       as tiltakstype_tiltakskode,
       arrangor.navn                 as arrangor_navn,
       arrangor.organisasjonsnummer  as arrangor_organisasjonsnummer,
       administratorer_json,
       nav_enheter_json,
       kontaktpersoner_json,
       arrangor_kontaktpersoner_json,
       ig.publisert,
       ig.sanity_id,
       ig.tiltaksnummer
from individuell_gjennomforing ig
         left join tiltakstype on tiltakstype.id = ig.tiltakstype_id
         left join arrangor on arrangor.id = ig.arrangor_id
         left join lateral (select jsonb_agg(
                                           jsonb_build_object(
                                                   'navIdent', ansatt.nav_ident,
                                                   'navn', concat(ansatt.fornavn, ' ', ansatt.etternavn)
                                           )
                                   ) as administratorer_json
                            from individuell_gjennomforing_administrator adm
                                     join nav_ansatt ansatt on ansatt.nav_ident = adm.nav_ident
                            where adm.individuell_gjennomforing_id = ig.id) on true
         left join lateral (select jsonb_agg(
                                           jsonb_build_object(
                                                   'enhetsnummer', enhet.enhetsnummer,
                                                   'navn', enhet.navn,
                                                   'type', enhet.type,
                                                   'overordnetEnhet', enhet.overordnet_enhet
                                           )
                                   ) as nav_enheter_json
                            from individuell_gjennomforing_nav_enhet ig_enhet
                                     join nav_enhet enhet on enhet.enhetsnummer = ig_enhet.enhetsnummer
                            where ig_enhet.individuell_gjennomforing_id = ig.id) on true
         left join lateral (select jsonb_agg(
                                           jsonb_build_object(
                                                   'navIdent', ansatt.nav_ident,
                                                   'navn', concat(ansatt.fornavn, ' ', ansatt.etternavn),
                                                   'epost', ansatt.epost,
                                                   'mobilnummer', ansatt.mobilnummer,
                                                   'hovedenhet', ansatt.hovedenhet,
                                                   'beskrivelse', kp.beskrivelse
                                           )
                                   ) as kontaktpersoner_json
                            from individuell_gjennomforing_kontaktperson kp
                                     join nav_ansatt ansatt on ansatt.nav_ident = kp.kontaktperson_nav_ident
                            where kp.individuell_gjennomforing_id = ig.id) on true
         left join lateral (select jsonb_agg(
                                           jsonb_build_object(
                                                   'id', akp.id,
                                                   'navn', akp.navn,
                                                   'telefon', akp.telefon,
                                                   'epost', akp.epost,
                                                   'beskrivelse', akp.beskrivelse
                                           )
                                   ) as arrangor_kontaktpersoner_json
                            from individuell_gjennomforing_arrangor_kontaktperson ig_akp
                                     join arrangor_kontaktperson akp on akp.id = ig_akp.arrangor_kontaktperson_id
                            where ig_akp.individuell_gjennomforing_id = ig.id) on true
