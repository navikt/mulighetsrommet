drop view if exists refusjonskrav_admin_dto_view;

create view refusjonskrav_admin_dto_view as
select
    refusjonskrav.id,
    tiltaksgjennomforing.id as tiltaksgjennomforing_id,
    tiltaksgjennomforing.navn as tiltaksgjennomforing_navn,
    tiltaksgjennomforing.tiltaksnummer as tiltaksgjennomforing_tiltaksnummer,
    refusjonskrav.periode_start,
    refusjonskrav.periode_slutt,
    refusjonskrav.beregning,
    arrangor.id                         as arrangor_id,
    arrangor.organisasjonsnummer        as arrangor_organisasjonsnummer,
    arrangor.navn                       as arrangor_navn,
    arrangor.slettet_dato is not null   as arrangor_slettet,
    avtale.navn as avtale_navn,
    avtale.id as avtale_id,
    tiltakstype.navn as tiltakstype_navn
from refusjonskrav
         inner join tiltaksgjennomforing on tiltaksgjennomforing.id = refusjonskrav.tiltaksgjennomforing_id
         inner join arrangor on arrangor.id = tiltaksgjennomforing.arrangor_id
         inner join avtale on tiltaksgjennomforing.avtale_id = avtale.id
         inner join tiltakstype on tiltaksgjennomforing.tiltakstype_id = tiltakstype.id
