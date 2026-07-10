drop view tiltaksgjennomforing_valid;

alter table tiltaksgjennomforing
    alter column navn drop not null;

create or replace view tiltaksgjennomforing_valid(id, navn, tiltaksnummer, tiltakskode, aar, tilgjengelighet) as
select id, navn, tiltaksnummer, tiltakskode, aar, tilgjengelighet
from tiltaksgjennomforing
where tiltaksnummer is not null
  and aar is not null
  and navn is not null;

