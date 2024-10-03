drop view if exists refusjonskrav_admin_dto_view;

create view refusjonskrav_admin_dto_view as
select refusjonskrav.id,
       refusjonskrav.periode_start,
       refusjonskrav.periode_slutt,
       refusjonskrav.beregning,
       gjennomforing.id                  as tiltaksgjennomforing_id,
       gjennomforing.navn                as tiltaksgjennomforing_navn,
       arrangor.id                       as arrangor_id,
       arrangor.organisasjonsnummer      as arrangor_organisasjonsnummer,
       arrangor.navn                     as arrangor_navn,
       arrangor.slettet_dato is not null as arrangor_slettet
from refusjonskrav
         inner join tiltaksgjennomforing gjennomforing on gjennomforing.id = refusjonskrav.tiltaksgjennomforing_id
         inner join arrangor on gjennomforing.arrangor_id = arrangor.id;
