-- ${flyway:timestamp}

drop view if exists tilsagn_arrangorflate_view;

create view tilsagn_arrangorflate_view as
select tilsagn.id,
       tilsagn.type,
       tilsagn.belop_gjenstaende,
       tilsagn.periode,
       tilsagn.status,
       tilsagn.prismodell,
       gjennomforing.navn           as gjennomforing_navn,
       gjennomforing.id             as gjennomforing_id,
       tiltakstype.navn             as tiltakstype_navn,
       arrangor.id                  as arrangor_id,
       arrangor.organisasjonsnummer as arrangor_organisasjonsnummer,
       arrangor.navn                as arrangor_navn
from tilsagn
         inner join gjennomforing on gjennomforing.id = tilsagn.gjennomforing_id
         inner join tiltakstype on tiltakstype.id = gjennomforing.tiltakstype_id
         inner join arrangor on arrangor.id = tilsagn.arrangor_id;
