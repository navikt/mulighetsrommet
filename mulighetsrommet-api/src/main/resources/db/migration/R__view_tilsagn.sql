-- ${flyway:timestamp}

drop view if exists view_tilsagn;

create view view_tilsagn as
select tilsagn.id,
       tilsagn.valuta,
       tilsagn.belop_brukt,
       tilsagn.belop_beregnet,
       tilsagn.periode,
       tilsagn.lopenummer,
       tilsagn.bestillingsnummer,
       tilsagn.bestilling_status,
       tilsagn.kostnadssted,
       tilsagn.kommentar,
       tilsagn.beskrivelse,
       tilsagn.status,
       tilsagn.type,
       tilsagn.beregning_type,
       tilsagn.beregning_sats,
       tilsagn.beregning_antall_plasser,
       tilsagn.beregning_antall_timer_oppfolging_per_deltaker,
       tilsagn.beregning_prisbetingelser,
       tilsagn.created_at,
       gjennomforing.id                  as gjennomforing_id,
       gjennomforing.lopenummer          as gjennomforing_lopenummer,
       gjennomforing.navn                as gjennomforing_navn,
       nav_enhet.navn                    as kostnadssted_navn,
       nav_enhet.overordnet_enhet        as kostnadssted_overordnet_enhet,
       nav_enhet.type                    as kostnadssted_type,
       nav_enhet.status                  as kostnadssted_status,
       arrangor.id                       as arrangor_id,
       arrangor.organisasjonsnummer      as arrangor_organisasjonsnummer,
       arrangor.navn                     as arrangor_navn,
       arrangor.slettet_dato is not null as arrangor_slettet,
       tiltakstype.tiltakskode           as tiltakskode,
       tiltakstype.navn                  as tiltakstype_navn
from tilsagn
         inner join nav_enhet on nav_enhet.enhetsnummer = tilsagn.kostnadssted
         inner join gjennomforing on gjennomforing.id = tilsagn.gjennomforing_id
         inner join arrangor on arrangor.id = gjennomforing.arrangor_id
         inner join tiltakstype on tiltakstype.id = gjennomforing.tiltakstype_id
