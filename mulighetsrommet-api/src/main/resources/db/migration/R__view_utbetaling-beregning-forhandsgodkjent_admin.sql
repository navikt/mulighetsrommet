-- ${flyway:timestamp}

drop view if exists view_utbetaling_beregning_manedsverk;

create view view_utbetaling_beregning_manedsverk as
with stengt_periode as (select utbetaling_id,
                               jsonb_agg(
                                       jsonb_build_object(
                                               'periode',
                                               jsonb_build_object(
                                                       'start', lower(periode),
                                                       'slutt', upper(periode)
                                               ),
                                               'beskrivelse', beskrivelse
                                       )
                               ) as stengt
                        from utbetaling_stengt_hos_arrangor
                        group by utbetaling_id),
     deltakelse_periode as (select utbetaling_id,
                                   deltakelse_id,
                                   jsonb_agg(
                                           jsonb_build_object(
                                                   'periode',
                                                   jsonb_build_object(
                                                           'start', lower(periode),
                                                           'slutt', upper(periode)
                                                   ),
                                                   'deltakelsesprosent',
                                                   deltakelsesprosent
                                           )
                                   ) as perioder
                            from utbetaling_deltakelse_periode
                            group by utbetaling_id, deltakelse_id),
     deltakelse_perioder as (select utbetaling_id,
                                    jsonb_agg(
                                            jsonb_build_object(
                                                    'deltakelseId', deltakelse_id,
                                                    'perioder', deltakelse_periode.perioder
                                            )
                                    ) as deltakelser
                             from deltakelse_periode
                             group by utbetaling_id),
     deltakelse_manedsverk as (select utbetaling_id,
                                      jsonb_agg(
                                              jsonb_build_object(
                                                      'deltakelseId', deltakelse_id,
                                                      'manedsverk', manedsverk
                                              )
                                      ) as deltakelser
                               from utbetaling_deltakelse_manedsverk
                               group by utbetaling_id)
select utbetaling.id,
       utbetaling.periode,
       utbetaling.belop_beregnet,
       beregning.sats,
       coalesce(deltakelse_manedsverk.deltakelser, '[]'::jsonb) as manedsverk_json,
       coalesce(stengt_periode.stengt, '[]'::jsonb)             as stengt_json,
       coalesce(deltakelse_perioder.deltakelser, '[]'::jsonb)   as perioder_json
from utbetaling
         join utbetaling_beregning_sats beregning on utbetaling.id = beregning.utbetaling_id
         left join deltakelse_manedsverk on beregning.utbetaling_id = deltakelse_manedsverk.utbetaling_id
         left join stengt_periode on beregning.utbetaling_id = stengt_periode.utbetaling_id
         left join deltakelse_perioder on beregning.utbetaling_id = deltakelse_perioder.utbetaling_id
