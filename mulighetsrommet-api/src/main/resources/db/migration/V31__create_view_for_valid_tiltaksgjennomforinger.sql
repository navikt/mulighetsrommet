alter table tiltaksgjennomforing
    alter column tiltaksnummer drop not null;

create view tiltaksgjennomforing_valid as
select id, navn, tiltaksnummer, tiltakskode, aar
from tiltaksgjennomforing
where tiltaksnummer is not null
  and aar is not null;
