update prismodell
set tilsagn_per_deltaker = false
where id in (select g.prismodell_id
             from gjennomforing g
                      join tilsagn t on t.gjennomforing_id = g.id
             where g.gjennomforing_type = 'ENKELTPLASS'
               and t.beregning_type = 'FRI');
