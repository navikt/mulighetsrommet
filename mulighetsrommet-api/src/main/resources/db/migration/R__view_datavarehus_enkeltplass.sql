drop view if exists view_datavarehus_enkeltplass;

create view view_datavarehus_enkeltplass as
select gjennomforing.id,
       gjennomforing.arena_tiltaksnummer,
       gjennomforing.created_at     as opprettet_tidspunkt,
       gjennomforing.updated_at     as oppdatert_tidspunkt,
       tiltakstype.tiltakskode      as tiltakstype_tiltakskode,
       arrangor.organisasjonsnummer as arrangor_organisasjonsnummer
from gjennomforing
         join tiltakstype on gjennomforing.tiltakstype_id = tiltakstype.id
         join arrangor on gjennomforing.arrangor_id = arrangor.id
