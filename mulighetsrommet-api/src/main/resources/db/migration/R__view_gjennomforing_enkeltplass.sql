drop view if exists view_gjennomforing_enkeltplass;

create view view_gjennomforing_enkeltplass as
select gjennomforing.id,
       gjennomforing.created_at          as opprettet_tidspunkt,
       gjennomforing.updated_at          as oppdatert_tidspunkt,
       gjennomforing.arena_tiltaksnummer,
       gjennomforing.arena_ansvarlig_enhet,
       gjennomforing.arena_navn,
       gjennomforing.arena_start_dato,
       gjennomforing.arena_slutt_dato,
       gjennomforing.arena_status,
       tiltakstype.id                    as tiltakstype_id,
       tiltakstype.navn                  as tiltakstype_navn,
       tiltakstype.tiltakskode           as tiltakstype_tiltakskode,
       arrangor.id                       as arrangor_id,
       arrangor.organisasjonsnummer      as arrangor_organisasjonsnummer,
       arrangor.navn                     as arrangor_navn,
       arrangor.slettet_dato is not null as arrangor_slettet
from gjennomforing
         join tiltakstype on gjennomforing.tiltakstype_id = tiltakstype.id
         join arrangor on arrangor.id = gjennomforing.arrangor_id
