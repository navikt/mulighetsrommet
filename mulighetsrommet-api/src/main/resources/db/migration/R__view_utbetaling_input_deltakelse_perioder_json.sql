create view view_utbetaling_input_deltakelse_perioder_json as
select utbetaling_id,
       jsonb_agg(
               jsonb_build_object(
                       'deltakelseId', deltakelse_id,
                       'periode',
                       jsonb_build_object(
                               'start', lower(periode),
                               'slutt', upper(periode)
                       )
               )
       ) as perioder_json
from utbetaling_deltakelse_periode
group by utbetaling_id
