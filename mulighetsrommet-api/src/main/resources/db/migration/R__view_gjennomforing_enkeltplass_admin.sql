drop view if exists view_gjennomforing_enkeltplass_admin;

create view view_gjennomforing_enkeltplass_admin as
select enkeltplass.id,
       enkeltplass.created_at            as opprettet_tidspunkt,
       enkeltplass.updated_at            as oppdatert_tidspunkt,
       enkeltplass.arena_tiltaksnummer,
       enkeltplass.arena_navn,
       enkeltplass.arena_start_dato,
       enkeltplass.arena_slutt_dato,
       enkeltplass.arena_status,
       enkeltplass.arena_ansvarlig_enhet,
       tiltakstype.id                    as tiltakstype_id,
       tiltakstype.navn                  as tiltakstype_navn,
       tiltakstype.tiltakskode           as tiltakstype_tiltakskode,
       arrangor.id                       as arrangor_id,
       arrangor.organisasjonsnummer      as arrangor_organisasjonsnummer,
       arrangor.navn                     as arrangor_navn,
       arrangor.slettet_dato is not null as arrangor_slettet
from enkeltplass
         join tiltakstype on enkeltplass.tiltakstype_id = tiltakstype.id
         join arrangor on arrangor.id = enkeltplass.arrangor_id
