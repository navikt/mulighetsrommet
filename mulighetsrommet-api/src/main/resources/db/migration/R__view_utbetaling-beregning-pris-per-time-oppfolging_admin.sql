-- ${flyway:timestamp}

drop view if exists view_utbetaling_beregning_pris_per_time_oppfolging;

create view view_utbetaling_beregning_pris_per_time_oppfolging as
with stengt as (select utbetaling_id,
                       jsonb_agg(
                               jsonb_build_object(
                                       'periode',
                                       jsonb_build_object(
                                               'start', lower(periode),
                                               'slutt', upper(periode)
                                       ),
                                       'beskrivelse', beskrivelse
                               )
                       ) as stengt_perioder_json
                from utbetaling_stengt_hos_arrangor
                group by utbetaling_id),
     deltakelser as (select utbetaling_id,
                            jsonb_agg(
                                    jsonb_build_object(
                                            'deltakelseId', deltakelse_id,
                                            'periode',
                                            jsonb_build_object(
                                                    'start', lower(periode),
                                                    'slutt', upper(periode)
                                            )
                                    )
                            ) as deltakelser_perioder_json
                     from utbetaling_deltakelse_periode
                     group by utbetaling_id)
select utbetaling.id,
       utbetaling.periode,
       utbetaling.belop_beregnet,
       beregning.sats,
       coalesce(stengt.stengt_perioder_json, '[]'::jsonb)           as stengt_perioder_json,
       coalesce(deltakelser.deltakelser_perioder_json, '[]'::jsonb) as deltakelser_perioder_json
from utbetaling
         join utbetaling_beregning_sats beregning on utbetaling.id = beregning.utbetaling_id
         left join stengt on utbetaling.id = stengt.utbetaling_id
         left join deltakelser on utbetaling.id = deltakelser.utbetaling_id
