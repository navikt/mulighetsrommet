-- ${flyway:timestamp}

drop view if exists view_utbetaling_input_stengt_json;

create view view_utbetaling_input_stengt_json as
select utbetaling_id,
       jsonb_agg(
               jsonb_build_object(
                       'periode',
                       jsonb_build_object(
                               'start', lower(periode),
                               'slutt', upper(periode)
                       ),
                       'beskrivelse', beskrivelse
               )
       ) as perioder_json
from utbetaling_stengt_hos_arrangor
group by utbetaling_id
