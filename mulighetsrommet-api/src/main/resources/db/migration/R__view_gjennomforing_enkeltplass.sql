-- ${flyway:timestamp}

drop view if exists view_gjennomforing_enkeltplass;

create view view_gjennomforing_enkeltplass as
select gjennomforing.id,
       gjennomforing.opphav,
       gjennomforing.created_at            as opprettet_tidspunkt,
       gjennomforing.updated_at            as oppdatert_tidspunkt,
       gjennomforing.lopenummer,
       gjennomforing.arena_tiltaksnummer,
       gjennomforing.arena_ansvarlig_enhet as arena_nav_enhet_enhetsnummer,
       gjennomforing.navn,
       gjennomforing.start_dato,
       gjennomforing.slutt_dato,
       gjennomforing.status,
       gjennomforing.avbrutt_aarsaker,
       gjennomforing.avbrutt_forklaring,
       gjennomforing.deltidsprosent,
       gjennomforing.antall_plasser,
       tiltakstype.id                      as tiltakstype_id,
       tiltakstype.navn                    as tiltakstype_navn,
       tiltakstype.tiltakskode             as tiltakstype_tiltakskode,
       arrangor.id                         as arrangor_id,
       arrangor.organisasjonsnummer        as arrangor_organisasjonsnummer,
       arrangor.navn                       as arrangor_navn,
       arrangor.slettet_dato is not null   as arrangor_slettet
from gjennomforing
         join tiltakstype on gjennomforing.tiltakstype_id = tiltakstype.id
         join arrangor on arrangor.id = gjennomforing.arrangor_id
where gjennomforing.gjennomforing_type = 'ENKELTPLASS'
