drop view if exists tilsagn_arrangorflate_view;

create view tilsagn_arrangorflate_view as
select
    tilsagn.id,
    tiltaksgjennomforing.navn as gjennomforing_navn,
    tiltaksgjennomforing.id as gjennomforing_id,
    tiltakstype.navn as tiltakstype_navn,
    tilsagn.periode_start,
    tilsagn.periode_slutt,
    tilsagn.beregning,
    arrangor.id                         as arrangor_id,
    arrangor.organisasjonsnummer        as arrangor_organisasjonsnummer,
    arrangor.navn                       as arrangor_navn
from tilsagn
    inner join tiltaksgjennomforing on tiltaksgjennomforing.id = tilsagn.tiltaksgjennomforing_id
    inner join tiltakstype on tiltakstype.id = tiltaksgjennomforing.tiltakstype_id
    inner join arrangor on arrangor.id = tilsagn.arrangor_id
where
    tilsagn.annullert_tidspunkt is null and
    tilsagn.besluttet_tidspunkt is not null

