-- ${flyway:timestamp}

drop view if exists view_utbetaling_output_deltakelse_perioder_json;

create view view_utbetaling_output_deltakelse_perioder_json as
with deltakelse_perioder as (select utbetaling_id,
                                    deltakelse_id,
                                    jsonb_agg(
                                            jsonb_build_object(
                                                    'periode',
                                                    jsonb_build_object(
                                                            'start', lower(periode),
                                                            'slutt', upper(periode)
                                                    ),
                                                    'faktor', faktor,
                                                    'sats',
                                                    jsonb_build_object(
                                                            'belop', sats,
                                                            'valuta', valuta
                                                    )
                                            )
                                    ) as perioder
                             from utbetaling_deltakelse_faktor
                             group by utbetaling_id, deltakelse_id)
select utbetaling_id,
       jsonb_agg(
               jsonb_build_object(
                       'deltakelseId', deltakelse_id,
                       'perioder', perioder
               )
       ) as perioder_json
from deltakelse_perioder
group by utbetaling_id
