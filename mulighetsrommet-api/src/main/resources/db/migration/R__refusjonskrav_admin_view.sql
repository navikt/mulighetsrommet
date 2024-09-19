drop view if exists refusjonskrav_admin_dto_view;

create view refusjonskrav_admin_dto_view as
select
    refusjonskrav.id,
    gjennomforing.id as gjennomforing_id,
    gjennomforing.navn as gjennomforing_navn,
    refusjonskrav.periode,
    refusjonskrav.beregning,
    arrangor.id                         as arrangor_id,
    arrangor.organisasjonsnummer        as arrangor_organisasjonsnummer,
    arrangor.navn                       as arrangor_navn,
    arrangor.slettet_dato is not null   as arrangor_slettet
from refusjonskrav
     inner join arrangor on arrangor.id = refusjonskrav.arrangor_id
     inner join tiltaksgjennomforing on tiltaksgjennomforing.id = refusjonskrav.tiltaksgjennomforing_id
