delete
from utbetaling
where date(created_at) = '2025-11-24'
  and status = 'GENERERT'
  and periode = daterange('2025-11-01', '2025-12-01')
