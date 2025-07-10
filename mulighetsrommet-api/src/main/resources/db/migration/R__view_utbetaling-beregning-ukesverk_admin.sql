-- ${flyway:timestamp}

drop view if exists view_utbetaling_beregning_ukesverk;

create view view_utbetaling_beregning_ukesverk as
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
     deltakelse_perioder as (select utbetaling_id,
                                    jsonb_agg(
                                            jsonb_build_object(
                                                    'deltakelseId', deltakelse_id,
                                                    'periode',
                                                    jsonb_build_object(
                                                            'start', lower(periode),
                                                            'slutt', upper(periode)
                                                    )
                                            )
                                    ) as deltakelser
                             from utbetaling_deltakelse_periode
                             group by utbetaling_id),
     deltakelse_ukesverk as (select utbetaling_id,
                                    jsonb_agg(
                                            jsonb_build_object(
                                                    'deltakelseId', deltakelse_id,
                                                    'ukesverk', faktor
                                            )
                                    ) as deltakelser
                             from utbetaling_deltakelse_faktor
                             group by utbetaling_id)
select utbetaling.id,
       utbetaling.periode,
       utbetaling.belop_beregnet,
       beregning.sats,
       coalesce(deltakelse_ukesverk.deltakelser, '[]'::jsonb) as ukesverk_json,
       coalesce(stengt_periode.stengt, '[]'::jsonb)           as stengt_json,
       coalesce(deltakelse_perioder.deltakelser, '[]'::jsonb) as perioder_json
from utbetaling
         join utbetaling_beregning_sats beregning on utbetaling.id = beregning.utbetaling_id
         left join deltakelse_ukesverk on utbetaling.id = deltakelse_ukesverk.utbetaling_id
         left join stengt_periode on utbetaling.id = stengt_periode.utbetaling_id
         left join deltakelse_perioder on utbetaling.id = deltakelse_perioder.utbetaling_id
