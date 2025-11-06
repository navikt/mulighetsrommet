drop view if exists view_datavarehus_gruppetiltak;

create view view_datavarehus_gruppetiltak as
select gjennomforing.id,
       gjennomforing.arena_tiltaksnummer,
       gjennomforing.created_at     as opprettet_tidspunkt,
       gjennomforing.updated_at     as oppdatert_tidspunkt,
       gruppe.navn,
       gruppe.start_dato,
       gruppe.slutt_dato,
       gruppe.status,
       gruppe.deltidsprosent,
       tiltakstype.tiltakskode      as tiltakstype_tiltakskode,
       avtale.id                    as avtale_id,
       avtale.navn                  as avtale_navn,
       avtale.created_at            as avtale_opprettet_tidspunkt,
       avtale.updated_at            as avtale_oppdatert_tidspunkt,
       arrangor.organisasjonsnummer as arrangor_organisasjonsnummer
from gjennomforing_gruppetiltak gruppe
         join gjennomforing on gruppe.gjennomforing_id = gjennomforing.id
         join tiltakstype on gjennomforing.tiltakstype_id = tiltakstype.id
         join arrangor on gjennomforing.arrangor_id = arrangor.id
         left join avtale on gruppe.avtale_id = avtale.id
