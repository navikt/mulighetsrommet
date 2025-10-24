-- ${flyway:timestamp}

drop view if exists view_utbetaling_beregning_ukesverk;

create view view_utbetaling_beregning_ukesverk as
with satser as (select utbetaling_id,
                       jsonb_agg(
                               jsonb_build_object(
                                       'periode',
                                       jsonb_build_object(
                                               'start', lower(periode),
                                               'slutt', upper(periode)
                                       ),
                                       'sats', sats
                               )
                       ) as sats_perioder_json
                from utbetaling_sats_periode
                group by utbetaling_id),
     stengt as (select utbetaling_id,
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
                     group by utbetaling_id),
     ukesverk as (select utbetaling_id,
                         jsonb_agg(
                                 jsonb_build_object(
                                         'deltakelseId', deltakelse_id,
                                         'perioder',
                                         (select jsonb_agg(
                                                         jsonb_build_object(
                                                                 'periode',
                                                                 jsonb_build_object(
                                                                         'start', lower(periode),
                                                                         'slutt', upper(periode)
                                                                 ),
                                                                 'faktor', faktor
                                                         )
                                                 )
                                          from utbetaling_deltakelse_faktor p2
                                          where p2.utbetaling_id = p1.utbetaling_id
                                            and p2.deltakelse_id = p1.deltakelse_id)
                                 )
                         ) as ukesverk_json
                  from utbetaling_deltakelse_faktor p1
                  group by utbetaling_id)
select utbetaling.id,
       utbetaling.periode,
       utbetaling.belop_beregnet,
       satser.sats_perioder_json,
       coalesce(ukesverk.ukesverk_json, '[]'::jsonb)                as ukesverk_json,
       coalesce(stengt.stengt_perioder_json, '[]'::jsonb)           as stengt_perioder_json,
       coalesce(deltakelser.deltakelser_perioder_json, '[]'::jsonb) as deltakelser_perioder_json
from utbetaling
         join satser on utbetaling.id = satser.utbetaling_id
         left join ukesverk on utbetaling.id = ukesverk.utbetaling_id
         left join stengt on utbetaling.id = stengt.utbetaling_id
         left join deltakelser on utbetaling.id = deltakelser.utbetaling_id
