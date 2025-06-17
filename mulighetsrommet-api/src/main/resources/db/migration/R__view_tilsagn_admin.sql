-- ${flyway:timestamp}

drop view if exists tilsagn_admin_dto_view;

create view tilsagn_admin_dto_view as
with tilsagn_totalt_utbetalt as ( -- Regn ut hva som faktisk er sendt til oebs
    select
        delutbetaling.tilsagn_id,
        sum(delutbetaling.belop) as belop_utbetalt
    from delutbetaling
    where delutbetaling.status = 'OVERFORT_TIL_UTBETALING'
      and delutbetaling.faktura_status = 'SENDT'
    group by delutbetaling.tilsagn_id
)
select tilsagn.id,
       tilsagn.gjennomforing_id,
       tilsagn.belop_brukt,
       tilsagn.belop_beregnet,
       coalesce(tilsagn_totalt_utbetalt.belop_utbetalt, 0) as belop_utbetalt,
       tilsagn.periode,
       tilsagn.lopenummer,
       tilsagn.bestillingsnummer,
       tilsagn.bestilling_status,
       tilsagn.kostnadssted,
       tilsagn.status,
       tilsagn.type,
       tilsagn.prismodell,
       tilsagn.created_at,
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
         inner join tiltakstype on tiltakstype.id = gjennomforing.tiltakstype_id
         inner join tilsagn_totalt_utbetalt on tilsagn_totalt_utbetalt.tilsagn_id = tilsagn.id
