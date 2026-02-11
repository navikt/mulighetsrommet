update gjennomforing
set gjennomforing_type = 'ARENA'
where gjennomforing_type = 'AVTALE'
  and (avtale_id is null or (slutt_dato is not null and slutt_dato < '2023-01-01'));

update gjennomforing
set gjennomforing_type = 'ARENA'
where gjennomforing_type = 'ENKELTPLASS'
  and (slutt_dato is not null and slutt_dato < '2026-01-01')
