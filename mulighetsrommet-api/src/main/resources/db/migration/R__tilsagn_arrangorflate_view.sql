drop view if exists tilsagn_arrangorflate_view;

create view tilsagn_arrangorflate_view as
select
    tilsagn.id,
    gjennomforing.navn as gjennomforing_navn,
    gjennomforing.id as gjennomforing_id,
    tiltakstype.navn as tiltakstype_navn,
    tilsagn.type,
    tilsagn.periode_start,
    tilsagn.periode_slutt,
    tilsagn.beregning,
    tilsagn.status,
    tilsagn.status_aarsaker,
    arrangor.id                         as arrangor_id,
    arrangor.organisasjonsnummer        as arrangor_organisasjonsnummer,
    arrangor.navn                       as arrangor_navn
from tilsagn
    inner join gjennomforing on gjennomforing.id = tilsagn.gjennomforing_id
    inner join tiltakstype on tiltakstype.id = gjennomforing.tiltakstype_id
    inner join arrangor on arrangor.id = tilsagn.arrangor_id
where
    tilsagn.status in ('GODKJENT', 'TIL_ANNULLERING', 'ANNULLERT')

