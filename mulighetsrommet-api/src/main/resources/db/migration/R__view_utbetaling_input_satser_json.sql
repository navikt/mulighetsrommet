create view view_utbetaling_input_satser_json as
select utbetaling_id,
       jsonb_agg(
               jsonb_build_object(
                       'periode',
                       jsonb_build_object(
                               'start', lower(periode),
                               'slutt', upper(periode)
                       ),
                       'sats',
                       jsonb_build_object(
                               'belop', sats,
                               'valuta', valuta
                       )
               )
       ) as perioder_json
from utbetaling_sats_periode
group by utbetaling_id
