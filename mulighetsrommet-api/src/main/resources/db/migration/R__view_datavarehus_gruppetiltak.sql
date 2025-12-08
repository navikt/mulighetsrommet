drop view if exists view_datavarehus_gruppetiltak;

create view view_datavarehus_gruppetiltak as
select gjennomforing.id,
       gjennomforing.navn,
       gjennomforing.start_dato,
       gjennomforing.slutt_dato,
       gjennomforing.status,
       gjennomforing.deltidsprosent,
       gjennomforing.arena_tiltaksnummer,
       gjennomforing.created_at     as opprettet_tidspunkt,
       gjennomforing.updated_at     as oppdatert_tidspunkt,
       tiltakstype.tiltakskode      as tiltakstype_tiltakskode,
       avtale.id                    as avtale_id,
       avtale.navn                  as avtale_navn,
       avtale.created_at            as avtale_opprettet_tidspunkt,
       avtale.updated_at            as avtale_oppdatert_tidspunkt,
       arrangor.organisasjonsnummer as arrangor_organisasjonsnummer
from gjennomforing
         join tiltakstype on gjennomforing.tiltakstype_id = tiltakstype.id
         left join avtale on gjennomforing.avtale_id = avtale.id
         join arrangor on gjennomforing.arrangor_id = arrangor.id
