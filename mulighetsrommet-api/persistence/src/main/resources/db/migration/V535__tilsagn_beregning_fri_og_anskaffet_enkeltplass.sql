insert into prismodell_type (value)
values ('ANSKAFFET_ENKELTPLASS');

insert into tilsagn_beregning_type (value)
values ('FRI');

update prismodell
set prismodell_type = 'ANSKAFFET_ENKELTPLASS'
where prismodell_type = 'ANNEN_AVTALT_PRIS'
  and id in (select prismodell_id from gjennomforing where gjennomforing_type = 'ENKELTPLASS');

update tilsagn
set beregning_type = 'FRI',
    beregning_sats = belop_beregnet
where beregning_type = 'ANNEN_AVTALT_PRIS'
  and gjennomforing_id in (select id from gjennomforing where gjennomforing_type = 'ENKELTPLASS');

delete
from tilsagn_annen_avtalt_pris_linje
where tilsagn_id in (select id
                     from tilsagn
                     where beregning_type = 'FRI'
                       and gjennomforing_id in (select id from gjennomforing where gjennomforing_type = 'ENKELTPLASS'));
