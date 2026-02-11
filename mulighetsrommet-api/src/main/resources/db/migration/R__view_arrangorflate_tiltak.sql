-- ${flyway:timestamp}

drop view if exists view_arrangorflate_tiltak;

create view view_arrangorflate_tiltak as
select gjennomforing.id,
       gjennomforing.lopenummer,
       gjennomforing.navn,
       gjennomforing.start_dato,
       gjennomforing.slutt_dato,
       gjennomforing.status,
       gjennomforing.avsluttet_tidspunkt,
       gjennomforing.avbrutt_aarsaker,
       gjennomforing.avbrutt_forklaring,
       tiltakstype.id               as tiltakstype_id,
       tiltakstype.navn             as tiltakstype_navn,
       tiltakstype.tiltakskode      as tiltakstype_tiltakskode,
       arrangor.id                  as arrangor_id,
       arrangor.organisasjonsnummer as arrangor_organisasjonsnummer,
       arrangor.navn                as arrangor_navn,
       prismodell.id                as prismodell_id,
       prismodell.valuta            as prismodell_valuta,
       prismodell.prismodell_type   as prismodell_type,
       prismodell.prisbetingelser   as prismodell_prisbetingelser,
       prismodell.satser            as prismodell_satser
from gjennomforing
         join tiltakstype on gjennomforing.tiltakstype_id = tiltakstype.id
         join arrangor on arrangor.id = gjennomforing.arrangor_id
         join prismodell on prismodell.id = gjennomforing.prismodell_id
where gjennomforing.avtale_id is not null
  and gjennomforing.gjennomforing_type = 'AVTALE'
