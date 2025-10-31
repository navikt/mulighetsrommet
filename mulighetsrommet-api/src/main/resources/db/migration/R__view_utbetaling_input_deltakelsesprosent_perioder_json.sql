create view view_utbetaling_input_deltakelsesprosent_perioder_json as
with deltakelse_periode as (select utbetaling_id,
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
                            group by utbetaling_id, deltakelse_id)
select utbetaling_id,
       jsonb_agg(
               jsonb_build_object(
                       'deltakelseId', deltakelse_id,
                       'perioder', deltakelse_periode.perioder
               )
       ) as perioder_json
from deltakelse_periode
group by utbetaling_id
