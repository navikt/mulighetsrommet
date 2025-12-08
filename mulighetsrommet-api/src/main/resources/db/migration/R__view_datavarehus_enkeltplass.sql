drop view if exists view_datavarehus_enkeltplass;

create view view_datavarehus_enkeltplass as
select enkeltplass.id,
       enkeltplass.arena_tiltaksnummer,
       enkeltplass.created_at       as opprettet_tidspunkt,
       enkeltplass.updated_at       as oppdatert_tidspunkt,
       tiltakstype.tiltakskode      as tiltakstype_tiltakskode,
       arrangor.organisasjonsnummer as arrangor_organisasjonsnummer
from enkeltplass
         join tiltakstype on enkeltplass.tiltakstype_id = tiltakstype.id
         join arrangor on enkeltplass.arrangor_id = arrangor.id
