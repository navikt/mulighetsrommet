-- For hver gjennomføring med type 'ENKELTPLASS', opprett en prismodell og koble til gjennomføringen
with new_prismodell as (
    insert into prismodell (id, prismodell_type, prisbetingelser, satser, system_id, valuta)
        select gen_random_uuid(), 'ANNEN_AVTALT_PRIS', null, null, null, 'NOK'
        from gjennomforing
        where gjennomforing_type = 'ENKELTPLASS'
        returning id),
     matched_gjennomforing as (select g.id                 as gjennomforing_id,
                                      p.id                 as prismodell_id,
                                      row_number() over () as rn_gjennomforing,
                                      row_number() over () as rn_prismodell
                               from gjennomforing g
                                        join new_prismodell p on true
                               where g.gjennomforing_type = 'ENKELTPLASS')
update gjennomforing g
set prismodell_id = m.prismodell_id
from matched_gjennomforing m
where g.id = m.gjennomforing_id
  and m.rn_gjennomforing = m.rn_prismodell;
