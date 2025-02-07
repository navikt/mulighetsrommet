drop view if exists utbetaling_aft_view;
drop view if exists utbetaling_dto_view;

create view utbetaling_dto_view as
select utbetaling.id,
       utbetaling.beregningsmodell,
       case
           when godkjent_av_arrangor_tidspunkt is not null then 'GODKJENT_AV_ARRANGOR'
           else 'KLAR_FOR_GODKJENNING'
           end::utbetaling_status     as status,
       utbetaling.frist_for_godkjenning,
       utbetaling.godkjent_av_arrangor_tidspunkt,
       utbetaling.kontonummer,
       utbetaling.kid,
       utbetaling.journalpost_id,
       lower(periode)                    as periode_start,
       upper(periode)                    as periode_slutt,
       gjennomforing.id                  as gjennomforing_id,
       gjennomforing.navn                as gjennomforing_navn,
       arrangor.id                       as arrangor_id,
       arrangor.organisasjonsnummer      as arrangor_organisasjonsnummer,
       arrangor.navn                     as arrangor_navn,
       arrangor.slettet_dato is not null as arrangor_slettet,
       tiltakstype.navn                  as tiltakstype_navn
from utbetaling
         inner join gjennomforing on gjennomforing.id = utbetaling.gjennomforing_id
         inner join arrangor on gjennomforing.arrangor_id = arrangor.id
         inner join tiltakstype on gjennomforing.tiltakstype_id = tiltakstype.id;

create view utbetaling_aft_view as
with deltakelse_perioder as (select utbetaling_id,
                                    deltakelse_id,
                                    jsonb_agg(jsonb_build_object(
                                            'start', lower(periode),
                                            'slutt', upper(periode),
                                            'deltakelsesprosent', deltakelsesprosent
                                              )) as perioder
                             from utbetaling_deltakelse_periode
                             group by utbetaling_id, deltakelse_id),
     krav_perioder as (select utbetaling_id,
                              jsonb_agg(jsonb_build_object(
                                      'deltakelseId', deltakelse_id,
                                      'perioder', deltakelse_perioder.perioder
                                        )) as deltakelser
                       from deltakelse_perioder
                       group by utbetaling_id),
     krav_manedsverk as (select utbetaling_id,
                                jsonb_agg(jsonb_build_object(
                                        'deltakelseId', deltakelse_id,
                                        'manedsverk', manedsverk
                                          )) as deltakelser
                         from utbetaling_deltakelse_manedsverk
                         group by utbetaling_id)
select utbetaling.*,
       beregning.utbetaling_id,
       beregning.belop,
       beregning.sats,
       lower(beregning.periode)                           as beregning_periode_start,
       upper(beregning.periode)                           as beregning_periode_slutt,
       coalesce(krav_perioder.deltakelser, '[]'::jsonb)   as perioder_json,
       coalesce(krav_manedsverk.deltakelser, '[]'::jsonb) as manedsverk_json
from utbetaling_dto_view utbetaling
         join
     utbetaling_beregning_aft beregning on utbetaling.id = beregning.utbetaling_id
         left join
     krav_perioder on beregning.utbetaling_id = krav_perioder.utbetaling_id
         left join
     krav_manedsverk on beregning.utbetaling_id = krav_manedsverk.utbetaling_id;
