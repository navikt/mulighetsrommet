-- ${flyway:timestamp}

drop view if exists view_utbetaling;

create view view_utbetaling as
select utbetaling.id,
       utbetaling.beregning_type,
       utbetaling.godkjent_av_arrangor_tidspunkt,
       utbetaling.utbetales_tidligst_tidspunkt,
       utbetaling.kontonummer,
       utbetaling.kid,
       utbetaling.journalpost_id,
       utbetaling.innsender,
       utbetaling.created_at,
       utbetaling.beskrivelse,
       utbetaling.begrunnelse_mindre_betalt,
       utbetaling.periode,
       utbetaling.tilskuddstype,
       utbetaling.status,
       utbetaling.belop_beregnet,
       utbetaling.avbrutt_begrunnelse,
       gjennomforing.id                  as gjennomforing_id,
       gjennomforing.lopenummer          as gjennomforing_lopenummer,
       gruppe.navn                       as gjennomforing_navn,
       arrangor.id                       as arrangor_id,
       arrangor.organisasjonsnummer      as arrangor_organisasjonsnummer,
       arrangor.navn                     as arrangor_navn,
       arrangor.slettet_dato is not null as arrangor_slettet,
       tiltakstype.navn                  as tiltakstype_navn,
       tiltakstype.id                    as tiltakstype_id,
       tiltakstype.tiltakskode
from utbetaling
         inner join gjennomforing on gjennomforing.id = utbetaling.gjennomforing_id
         inner join arrangor on gjennomforing.arrangor_id = arrangor.id
         inner join tiltakstype on gjennomforing.tiltakstype_id = tiltakstype.id
         left join gjennomforing_gruppetiltak gruppe on gruppe.gjennomforing_id = gjennomforing.id
