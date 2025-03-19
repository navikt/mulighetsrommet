-- ${flyway:timestamp}

drop view if exists tilsagn_admin_dto_view;

create view tilsagn_admin_dto_view as
select tilsagn.id,
       tilsagn.gjennomforing_id,
       tilsagn.belop_gjenstaende,
       tilsagn.belop_beregnet,
       tilsagn.periode,
       tilsagn.lopenummer,
       tilsagn.bestillingsnummer,
       tilsagn.kostnadssted,
       tilsagn.status,
       tilsagn.type,
       tilsagn.prismodell,
       nav_enhet.navn                    as kostnadssted_navn,
       nav_enhet.overordnet_enhet        as kostnadssted_overordnet_enhet,
       nav_enhet.type                    as kostnadssted_type,
       nav_enhet.status                  as kostnadssted_status,
       arrangor.id                       as arrangor_id,
       arrangor.organisasjonsnummer      as arrangor_organisasjonsnummer,
       arrangor.navn                     as arrangor_navn,
       arrangor.slettet_dato is not null as arrangor_slettet,
       gjennomforing.tiltaksnummer       as tiltaksnummer,
       gjennomforing.navn                as gjennomforing_navn,
       tiltakstype.tiltakskode           as tiltakskode,
       tiltakstype.navn                  as tiltakstype_navn
from tilsagn
         inner join nav_enhet on nav_enhet.enhetsnummer = tilsagn.kostnadssted
         inner join gjennomforing on gjennomforing.id = tilsagn.gjennomforing_id
         inner join arrangor on arrangor.id = gjennomforing.arrangor_id
         inner join tiltakstype on tiltakstype.id = gjennomforing.tiltakstype_id;
