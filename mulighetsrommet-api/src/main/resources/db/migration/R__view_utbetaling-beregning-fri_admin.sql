-- ${flyway:timestamp}

drop view if exists view_utbetaling_beregning_fri;

create view view_utbetaling_beregning_fri as
with deltakelser as (select utbetaling_id,
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
                     group by utbetaling_id),
     fri as (select utbetaling_id,
                         jsonb_agg(
                                 jsonb_build_object(
                                         'deltakelseId', deltakelse_id,
                                         'faktor', faktor
                                 )
                         ) as fri_json
                  from utbetaling_deltakelse_faktor
                  group by utbetaling_id)
select utbetaling.id,
       utbetaling.periode,
       utbetaling.belop_beregnet,
       coalesce(fri.fri_json, '[]'::jsonb)                as fri_json,
       coalesce(deltakelser.deltakelser_perioder_json, '[]'::jsonb) as deltakelser_perioder_json
from utbetaling
         left join fri on utbetaling.id = fri.utbetaling_id
         left join deltakelser on utbetaling.id = deltakelser.utbetaling_id
