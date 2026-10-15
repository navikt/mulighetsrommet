create or replace view view_gjennomforing_kompakt as
select gjennomforing.id,
       gjennomforing.gjennomforing_type,
       gjennomforing.lopenummer,
       gjennomforing.navn,
       gjennomforing.start_dato,
       gjennomforing.slutt_dato,
       gjennomforing.status,
       gjennomforing.avbrutt_aarsaker,
       gjennomforing.avbrutt_forklaring,
       gjennomforing.publisert,
       gjennomforing.avtale_id,
       gjennomforing.fts,
       tiltakstype.id               as tiltakstype_id,
       tiltakstype.navn             as tiltakstype_navn,
       tiltakstype.tiltakskode      as tiltakstype_tiltakskode,
       arrangor.id                  as arrangor_id,
       arrangor.organisasjonsnummer as arrangor_organisasjonsnummer,
       arrangor.navn                as arrangor_navn,
       nav_enheter_json,
       ansvarlig_enhet.enhetsnummer             as ansvarlig_enhet_enhetsnummer,
       ansvarlig_enhet.navn                     as ansvarlig_enhet_navn,
       ansvarlig_enhet.type                     as ansvarlig_enhet_type,
       ansvarlig_enhet_overordnet.enhetsnummer  as ansvarlig_enhet_overordnet_enhetsnummer,
       ansvarlig_enhet_overordnet.navn          as ansvarlig_enhet_overordnet_navn
from gjennomforing
         join tiltakstype on gjennomforing.tiltakstype_id = tiltakstype.id
         join arrangor on arrangor.id = gjennomforing.arrangor_id
         left join nav_enhet ansvarlig_enhet on ansvarlig_enhet.enhetsnummer = gjennomforing.ansvarlig_enhet
         left join nav_enhet ansvarlig_enhet_overordnet
                   on ansvarlig_enhet_overordnet.enhetsnummer = ansvarlig_enhet.overordnet_enhet
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
                            where gjennomforing_id = gjennomforing.id) on true;
