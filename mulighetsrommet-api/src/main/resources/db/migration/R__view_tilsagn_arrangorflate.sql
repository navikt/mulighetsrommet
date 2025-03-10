-- ${flyway:timestamp}

drop view if exists tilsagn_arrangorflate_view;

create view tilsagn_arrangorflate_view as
select tilsagn.id,
       gjennomforing.navn                              as gjennomforing_navn,
       gjennomforing.id                                as gjennomforing_id,
       tiltakstype.navn                                as tiltakstype_navn,
       tilsagn.type,
       lower(tilsagn.periode)                          as periode_start,
       date(upper(tilsagn.periode) - interval '1 day') as periode_slutt,
       tilsagn.beregning,
       tilsagn.status,
       arrangor.id                                     as arrangor_id,
       arrangor.organisasjonsnummer                    as arrangor_organisasjonsnummer,
       arrangor.navn                                   as arrangor_navn
from tilsagn
         inner join gjennomforing on gjennomforing.id = tilsagn.gjennomforing_id
         inner join tiltakstype on tiltakstype.id = gjennomforing.tiltakstype_id
         inner join arrangor on arrangor.id = tilsagn.arrangor_id;
