create or replace view view_arrangorflate_utbetaling_kompakt as
select utbetaling.id,
       utbetaling.beregning_type,
       utbetaling.periode,
       utbetaling.tilskuddstype,
       utbetaling.status,
       utbetaling.belop_beregnet,
       utbetaling.valuta,
       utbetaling.korreksjon_gjelder_utbetaling_id,
       utbetaling_linjer.sum_utbetaling_linje,
       blokkeringer,
       gjennomforing.id             as gjennomforing_id,
       gjennomforing.navn           as gjennomforing_navn,
       gjennomforing.lopenummer     as gjennomforing_lopenummer,
       gjennomforing.fts            as gjennomforing_fts,
       arrangor.id                  as arrangor_id,
       arrangor.organisasjonsnummer as arrangor_organisasjonsnummer,
       arrangor.navn                as arrangor_navn,
       tiltakstype.navn             as tiltakstype_navn,
       tiltakstype.tiltakskode
from utbetaling
         inner join gjennomforing on gjennomforing.id = utbetaling.gjennomforing_id
         inner join arrangor on gjennomforing.arrangor_id = arrangor.id
         inner join tiltakstype on gjennomforing.tiltakstype_id = tiltakstype.id
         left join lateral (select coalesce(array_agg(blokkering), '{}') as blokkeringer
                            from utbetaling_blokkering
                            where utbetaling_id = utbetaling.id) blokkeringer on true
         left join lateral (select sum(ul.belop) as sum_utbetaling_linje
                            from utbetaling_linje ul
                            where ul.utbetaling_id = utbetaling.id) utbetaling_linjer on true
where gjennomforing.avtale_id is not null
  and gjennomforing.gjennomforing_type = 'AVTALE';
