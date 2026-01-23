-- En beregning for en gjennomføring som ble avsluttet i 2015 ble trigget etter at en deltaker ble oppdatert
delete
from scheduled_tasks
where task_name = 'OppdaterUtbetalingBeregning'
  and consecutive_failures > 20;
