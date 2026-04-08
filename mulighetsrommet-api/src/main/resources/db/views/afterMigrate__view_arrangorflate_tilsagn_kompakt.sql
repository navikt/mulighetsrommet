create or replace view view_arrangorflate_tilsagn_kompakt as
select tilsagn.id,
       tilsagn.periode,
       tilsagn.bestillingsnummer,
       tilsagn.status,
       tilsagn.tilsagn_type,
       tilsagn.fts,
       gjennomforing.lopenummer     as gjennomforing_lopenummer,
       gjennomforing.navn           as gjennomforing_navn,
       arrangor.organisasjonsnummer as arrangor_organisasjonsnummer,
       arrangor.navn                as arrangor_navn,
       tiltakstype.tiltakskode      as tiltakskode,
       tiltakstype.navn             as tiltakstype_navn
from tilsagn
         inner join nav_enhet on nav_enhet.enhetsnummer = tilsagn.kostnadssted
         inner join gjennomforing on gjennomforing.id = tilsagn.gjennomforing_id
         inner join arrangor on arrangor.id = gjennomforing.arrangor_id
         inner join tiltakstype on tiltakstype.id = gjennomforing.tiltakstype_id
